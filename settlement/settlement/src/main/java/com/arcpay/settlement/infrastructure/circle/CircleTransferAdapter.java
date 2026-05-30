package com.arcpay.settlement.infrastructure.circle;

import com.arcpay.settlement.domain.InsufficientBalanceException;
import com.arcpay.settlement.domain.model.SettlementTransaction;
import com.arcpay.settlement.domain.model.TransferCommand;
import com.arcpay.settlement.domain.model.TransferState;
import com.arcpay.settlement.domain.model.TransferStatus;
import com.arcpay.settlement.domain.model.TransferSubmission;
import com.arcpay.settlement.domain.model.WalletBalance;
import com.arcpay.settlement.domain.port.CustodyProvider;
import com.arcpay.settlement.domain.port.SettlementTransactionRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.arcpay.settlement.domain.model.TransferState.INITIATED;

@Slf4j
@Component
@RequiredArgsConstructor
class CircleTransferAdapter implements CustodyProvider {

    private static final UUID IDEMPOTENCY_NAMESPACE =
            UUID.fromString("a3f1b2c4-5d6e-4f80-9a1b-2c3d4e5f6071");
    private static final String FEE_LEVEL = "MEDIUM";

    private final CircleApiProperties properties;
    private final SettlementProperties settlementProperties;
    private final RestClient restClient;
    private final EntitySecretCiphertextProvider ciphertextProvider;
    private final SettlementTransactionRepository repository;

    @Override
    public TransferSubmission submitTransfer(TransferCommand command) {
        var existing = repository.findByPaymentId(command.paymentId());
        if (existing.isPresent()) {
            var found = existing.get();
            log.info("Replay for paymentId={} returns existing circleTxId={}",
                    command.paymentId(), found.circleTxId());
            return new TransferSubmission(found.circleTxId(), found.state());
        }

        guardBalance(command);

        var circleTxId = submitToCircle(command);

        var transaction = SettlementTransaction.builder()
                .paymentId(command.paymentId())
                .circleTxId(circleTxId)
                .state(INITIATED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        var persisted = repository.save(transaction);

        return new TransferSubmission(persisted.circleTxId(), persisted.state());
    }

    @Override
    public TransferStatus getStatus(String circleTxId) {
        try {
            var response = restClient.get()
                    .uri("/v1/w3s/transactions/{circleTxId}", circleTxId)
                    .retrieve()
                    .body(TransactionResponse.class);

            if (response == null || response.data() == null || response.data().transaction() == null) {
                throw new CircleApiException("Empty transaction response from Circle API for circleTxId="
                        + circleTxId);
            }

            var transaction = response.data().transaction();
            return TransferStatus.builder()
                    .circleTxId(circleTxId)
                    .txHash(transaction.txHash())
                    .state(TransferState.valueOf(transaction.state()))
                    .networkFee(networkFee(transaction.networkFee()))
                    .errorReason(transaction.errorReason())
                    .build();
        } catch (CircleApiException e) {
            throw e;
        } catch (Exception e) {
            throw new CircleApiException("Circle transaction status query failed for circleTxId="
                    + circleTxId, e);
        }
    }

    @Override
    public WalletBalance getBalance(String walletId) {
        return fetchBalance(walletId);
    }

    private BigDecimal networkFee(String networkFee) {
        return networkFee == null ? null : new BigDecimal(networkFee);
    }

    private void guardBalance(TransferCommand command) {
        var balance = fetchBalance(command.walletId());
        var required = command.amount().add(settlementProperties.gasBufferUsdc());
        if (balance.amount().compareTo(required) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance for paymentId=" + command.paymentId()
                            + " walletId=" + command.walletId()
                            + " balance=" + balance.amount() + " required=" + required);
        }
    }

    private WalletBalance fetchBalance(String walletId) {
        try {
            var response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/w3s/wallets/{walletId}/balances")
                            .queryParam("tokenAddress", properties.usdcTokenAddress())
                            .build(walletId))
                    .retrieve()
                    .body(BalancesResponse.class);

            if (response == null || response.data() == null || response.data().tokenBalances() == null
                    || response.data().tokenBalances().isEmpty()) {
                return new WalletBalance(walletId, properties.usdcTokenAddress(), BigDecimal.ZERO);
            }

            var amount = new BigDecimal(response.data().tokenBalances().getFirst().amount());
            return new WalletBalance(walletId, properties.usdcTokenAddress(), amount);
        } catch (CircleApiException e) {
            throw e;
        } catch (Exception e) {
            throw new CircleApiException("Circle balance query failed for walletId=" + walletId, e);
        }
    }

    private String submitToCircle(TransferCommand command) {
        var requestBody = Map.of(
                "walletId", command.walletId(),
                "destinationAddress", command.recipientAddress(),
                "amounts", List.of(command.amount().toPlainString()),
                "tokenAddress", properties.usdcTokenAddress(),
                "blockchain", properties.blockchain(),
                "feeLevel", FEE_LEVEL,
                "idempotencyKey", idempotencyKey(command.paymentId()).toString(),
                "entitySecretCiphertext", ciphertextProvider.generate(),
                "refId", command.paymentId().toString()
        );

        try {
            var response = restClient.post()
                    .uri("/v1/w3s/developer/transactions/transfer")
                    .body(requestBody)
                    .retrieve()
                    .body(TransferResponse.class);

            if (response == null || response.data() == null || response.data().id() == null) {
                throw new CircleApiException("Empty transfer response from Circle API for paymentId="
                        + command.paymentId());
            }

            log.info("Circle transfer submitted paymentId={} circleTxId={}",
                    command.paymentId(), response.data().id());
            return response.data().id();
        } catch (CircleApiException e) {
            throw e;
        } catch (Exception e) {
            throw new CircleApiException("Circle transfer submission failed for paymentId="
                    + command.paymentId(), e);
        }
    }

    static UUID idempotencyKey(UUID paymentId) {
        return UuidCreator.getNameBasedSha1(IDEMPOTENCY_NAMESPACE, paymentId.toString());
    }

    record TransferResponse(TransferData data) {
        record TransferData(String id, String state) {}
    }

    record BalancesResponse(BalancesData data) {
        record BalancesData(List<TokenBalance> tokenBalances) {
            record TokenBalance(String amount) {}
        }
    }

    record TransactionResponse(TransactionData data) {
        record TransactionData(Transaction transaction) {
            record Transaction(String state, String txHash, String networkFee, String errorReason) {}
        }
    }
}

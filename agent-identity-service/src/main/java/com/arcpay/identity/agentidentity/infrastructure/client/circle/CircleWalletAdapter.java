package com.arcpay.identity.agentidentity.infrastructure.client.circle;

import com.arcpay.identity.agentidentity.domain.port.CircleWalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@EnableConfigurationProperties(CircleApiProperties.class)
@ConditionalOnProperty(prefix = "circle.api", name = "base-url")
class CircleWalletAdapter implements CircleWalletService {

    private final RestClient restClient;
    private final CircleApiProperties properties;

    CircleWalletAdapter(CircleApiProperties properties) {
        this.properties = properties;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutMs());
        requestFactory.setReadTimeout(properties.readTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public WalletCreationResult createWallet(UUID agentId) {
        var requestBody = Map.of(
                "idempotencyKey", agentId.toString(),
                "walletSetId", properties.walletSetId(),
                "blockchains", new String[]{properties.blockchain()},
                "count", 1
        );

        try {
            var response = restClient.post()
                    .uri("/v1/w3s/developer/wallets")
                    .body(requestBody)
                    .retrieve()
                    .body(CreateWalletResponse.class);

            if (response == null || response.data() == null || response.data().wallets() == null
                    || response.data().wallets().isEmpty()) {
                throw new CircleApiException("Empty wallet response from Circle API for agentId=" + agentId);
            }

            var wallet = response.data().wallets().getFirst();
            var normalizedAddress = wallet.address().toLowerCase(Locale.ROOT);
            log.info("Circle wallet created agentId={} walletId={}", agentId, wallet.id());
            return new WalletCreationResult(wallet.id(), normalizedAddress);
        } catch (CircleApiException e) {
            throw e;
        } catch (Exception e) {
            throw new CircleApiException("Circle wallet creation failed for agentId=" + agentId, e);
        }
    }

    record CreateWalletResponse(WalletData data) {
        record WalletData(java.util.List<Wallet> wallets) {
            record Wallet(String id, String address, String blockchain) {}
        }
    }
}

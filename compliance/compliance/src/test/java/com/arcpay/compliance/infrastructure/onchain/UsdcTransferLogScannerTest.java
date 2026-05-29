package com.arcpay.compliance.infrastructure.onchain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthLog;

import java.math.BigInteger;
import java.util.List;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_CLEAN_COUNTERPARTY;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_RECIPIENT_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SANCTIONED_ADDRESS;
import static com.arcpay.compliance.fixtures.OnChainFixtures.SOME_USDC_CONTRACT;
import static com.arcpay.compliance.fixtures.OnChainFixtures.ethLogResponse;
import static com.arcpay.compliance.fixtures.OnChainFixtures.transferLog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UsdcTransferLogScannerTest {

    private static final long SCAN_WINDOW = 50000;
    private static final BigInteger LATEST_BLOCK = BigInteger.valueOf(100000);
    private static final BigInteger EXPECTED_FROM_BLOCK = BigInteger.valueOf(50000);

    @Mock
    private Web3j web3j;

    @Mock
    private Request<?, EthBlockNumber> blockNumberRequest;

    @Mock
    private Request<?, EthLog> ethLogRequest;

    private final OnChainProperties properties =
            new OnChainProperties("http://localhost:8545", SOME_USDC_CONTRACT, SCAN_WINDOW, List.of(), 50);

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnCounterpartyAndUseBoundedBlockRange() throws Exception {
        // given
        givenLatestBlock();
        var filterCaptor = ArgumentCaptor.forClass(EthFilter.class);
        var fromQuery = ethLogResponse(List.of(transferLog(SOME_RECIPIENT_ADDRESS, SOME_SANCTIONED_ADDRESS)));
        var toQuery = ethLogResponse(List.of());
        given(web3j.ethGetLogs(filterCaptor.capture()))
                .willReturn((Request) ethLogRequest);
        given(ethLogRequest.send()).willReturn(fromQuery, toQuery);
        var scanner = new UsdcTransferLogScanner(web3j, properties);

        // when
        var counterparties = scanner.counterpartiesOf(SOME_RECIPIENT_ADDRESS);

        // then
        assertThat(counterparties).containsExactly(SOME_SANCTIONED_ADDRESS);
        var firstFilter = filterCaptor.getAllValues().getFirst();
        assertThat(((DefaultBlockParameterNumber) firstFilter.getFromBlock()).getBlockNumber())
                .isEqualTo(EXPECTED_FROM_BLOCK);
        assertThat(((DefaultBlockParameterNumber) firstFilter.getToBlock()).getBlockNumber())
                .isEqualTo(LATEST_BLOCK);
        assertThat(firstFilter.getAddress()).containsExactly(SOME_USDC_CONTRACT);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyWhenNoLogsInWindow() throws Exception {
        // given
        givenLatestBlock();
        given(web3j.ethGetLogs(org.mockito.ArgumentMatchers.any())).willReturn((Request) ethLogRequest);
        given(ethLogRequest.send()).willReturn(ethLogResponse(List.of()), ethLogResponse(List.of()));
        var scanner = new UsdcTransferLogScanner(web3j, properties);

        // when
        var counterparties = scanner.counterpartiesOf(SOME_CLEAN_COUNTERPARTY);

        // then
        assertThat(counterparties).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private void givenLatestBlock() throws Exception {
        var blockNumber = new EthBlockNumber();
        blockNumber.setResult("0x186a0");
        given(web3j.ethBlockNumber()).willReturn((Request) blockNumberRequest);
        given(blockNumberRequest.send()).willReturn(blockNumber);
    }
}

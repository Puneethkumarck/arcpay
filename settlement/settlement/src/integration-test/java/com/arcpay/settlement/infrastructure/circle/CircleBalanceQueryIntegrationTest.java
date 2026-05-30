package com.arcpay.settlement.infrastructure.circle;

import com.arcpay.settlement.domain.model.WalletBalance;
import com.arcpay.settlement.domain.port.CustodyProvider;
import com.arcpay.settlement.test.FullContextIntegrationTest;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static com.arcpay.settlement.fixtures.CircleKeyFixtures.publicKeyPem;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_WALLET_ID;
import static com.arcpay.settlement.test.stubs.CircleStubs.stubEmptyWalletBalance;
import static com.arcpay.settlement.test.stubs.CircleStubs.stubEntityPublicKey;
import static com.arcpay.settlement.test.stubs.CircleStubs.stubWalletBalance;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class CircleBalanceQueryIntegrationTest extends FullContextIntegrationTest {

    private static final String USDC_TOKEN_ADDRESS = "0x3600000000000000000000000000000000000000";
    private static final String BALANCES_PATH = "/v1/w3s/wallets/" + SOME_WALLET_ID + "/balances";

    @Autowired
    private CustodyProvider custodyProvider;

    private WireMockServer circleServer;

    @BeforeEach
    void setUp() {
        circleServer = new WireMockServer(options().port(8089));
        circleServer.start();
        stubEntityPublicKey(circleServer, publicKeyPem());
    }

    @AfterEach
    void tearDown() {
        circleServer.stop();
    }

    @Test
    void shouldReturnUsdcBalanceFromCircle() {
        // given
        stubWalletBalance(circleServer, SOME_WALLET_ID, "120.500000");

        // when
        var balance = custodyProvider.getBalance(SOME_WALLET_ID);

        // then
        var expected = new WalletBalance(SOME_WALLET_ID, USDC_TOKEN_ADDRESS, new BigDecimal("120.500000"));
        assertThat(balance).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnZeroBalanceWhenNoTokenBalances() {
        // given
        stubEmptyWalletBalance(circleServer, SOME_WALLET_ID);

        // when
        var balance = custodyProvider.getBalance(SOME_WALLET_ID);

        // then
        var expected = new WalletBalance(SOME_WALLET_ID, USDC_TOKEN_ADDRESS, BigDecimal.ZERO);
        assertThat(balance).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldQueryBalancesWithUsdcTokenAddressParam() {
        // given
        stubWalletBalance(circleServer, SOME_WALLET_ID, "10.000000");

        // when
        custodyProvider.getBalance(SOME_WALLET_ID);

        // then
        circleServer.verify(getRequestedFor(urlPathEqualTo(BALANCES_PATH))
                .withQueryParam("tokenAddress", com.github.tomakehurst.wiremock.client.WireMock.equalTo(USDC_TOKEN_ADDRESS)));
    }
}

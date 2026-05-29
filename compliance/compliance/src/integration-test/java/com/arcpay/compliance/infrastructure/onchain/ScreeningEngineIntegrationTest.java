package com.arcpay.compliance.infrastructure.onchain;

import com.arcpay.compliance.domain.model.CheckType;
import com.arcpay.compliance.domain.model.Verdict;
import com.arcpay.compliance.domain.port.SanctionsSetProvider;
import com.arcpay.compliance.domain.port.ScreeningEngine;
import com.arcpay.compliance.domain.port.WatchlistStore;
import com.arcpay.compliance.test.FullContextIntegrationTest;
import com.github.f4b6a3.uuid.UuidCreator;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.UUID;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_AGENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_PAYMENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_RECIPIENT_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SANCTIONED_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_WATCHLIST_ADDRESS;
import static com.arcpay.compliance.fixtures.OnChainFixtures.blockNumberJson;
import static com.arcpay.compliance.fixtures.OnChainFixtures.emptyLogsJson;
import static com.arcpay.compliance.fixtures.OnChainFixtures.transferLogsJson;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = {
        "compliance.sanctions.poll-interval-ms=500",
        "compliance.onchain.scan-block-window=50000"
})
class ScreeningEngineIntegrationTest extends FullContextIntegrationTest {

    private static final long LATEST_BLOCK = 100000;
    private static final long EXPECTED_FROM_BLOCK = 50000;

    private static WireMockServer rpcServer;

    @Autowired
    private ScreeningEngine screeningEngine;

    @Autowired
    private WatchlistStore watchlistStore;

    @Autowired
    private SanctionsSetProvider sanctionsSetProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void startRpcStub() {
        rpcServer = new WireMockServer(0);
        rpcServer.start();
    }

    @AfterAll
    static void stopRpcStub() {
        if (rpcServer != null) {
            rpcServer.stop();
        }
    }

    @DynamicPropertySource
    static void onChainProperties(DynamicPropertyRegistry registry) {
        registry.add("compliance.onchain.rpc-url", () -> "http://localhost:" + rpcServer.port());
    }

    @BeforeEach
    void resetState() {
        rpcServer.resetAll();
        rpcServer.stubFor(post(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", WireMock.equalTo("eth_blockNumber")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(blockNumberJson(LATEST_BLOCK))));
        jdbcTemplate.update("DELETE FROM watchlist_address");
    }

    @Test
    void shouldBlockSanctionedRecipient() {
        // given
        var versionId = seedSanctions(SOME_SANCTIONED_ADDRESS);
        awaitSanctionsLoaded(versionId);
        stubEmptyLogs();

        // when
        var result = screeningEngine.screen(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_SANCTIONED_ADDRESS);

        // then
        assertThat(result.verdict()).isEqualTo(Verdict.BLOCK);
        assertThat(result.checks()).anyMatch(check -> check.type() == CheckType.SANCTIONS_OFAC);
    }

    @Test
    void shouldHoldWatchlistedRecipientWithScoreHundred() {
        // given
        var versionId = seedSanctions(SOME_SANCTIONED_ADDRESS);
        awaitSanctionsLoaded(versionId);
        watchlistStore.addAddress(SOME_WATCHLIST_ADDRESS, "operator-flagged", "officer@arcpay.io");
        stubEmptyLogs();

        // when
        var result = screeningEngine.screen(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_WATCHLIST_ADDRESS);

        // then
        assertThat(result.verdict()).isEqualTo(Verdict.HOLD);
        assertThat(result.riskScore()).isEqualTo(100);
    }

    @Test
    void shouldHoldOnOneHopSanctionedInteractionUsingBoundedBlockRange() {
        // given
        var versionId = seedSanctions(SOME_SANCTIONED_ADDRESS);
        awaitSanctionsLoaded(versionId);
        stubLogsFor(SOME_RECIPIENT_ADDRESS, SOME_SANCTIONED_ADDRESS);

        // when
        var result = screeningEngine.screen(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT_ADDRESS);

        // then
        assertThat(result.verdict()).isEqualTo(Verdict.HOLD);
        assertThat(result.riskScore()).isEqualTo(70);
        assertThat(result.listVersionId()).isEqualTo(versionId);
        rpcServer.verify(postRequestedFor(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", WireMock.equalTo("eth_getLogs")))
                .withRequestBody(matchingJsonPath("$.params[0].fromBlock",
                        WireMock.equalTo("0x" + Long.toHexString(EXPECTED_FROM_BLOCK))))
                .withRequestBody(matchingJsonPath("$.params[0].toBlock",
                        WireMock.equalTo("0x" + Long.toHexString(LATEST_BLOCK)))));
    }

    @Test
    void shouldPassCleanRecipient() {
        // given
        var versionId = seedSanctions(SOME_SANCTIONED_ADDRESS);
        awaitSanctionsLoaded(versionId);
        stubEmptyLogs();

        // when
        var result = screeningEngine.screen(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT_ADDRESS);

        // then
        assertThat(result.verdict()).isEqualTo(Verdict.PASS);
    }

    private void stubEmptyLogs() {
        rpcServer.stubFor(post(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", WireMock.equalTo("eth_getLogs")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(emptyLogsJson())));
    }

    private void stubLogsFor(String recipient, String counterparty) {
        rpcServer.stubFor(post(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", WireMock.equalTo("eth_getLogs")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(transferLogsJson(recipient, counterparty))));
    }

    private void awaitSanctionsLoaded(UUID versionId) {
        await().atMost(Duration.ofSeconds(10))
                .until(() -> versionId.equals(sanctionsSetProvider.getCurrentSanctionsSet().versionId()));
    }

    private UUID seedSanctions(String address) {
        var versionId = UuidCreator.getTimeOrderedEpoch();
        jdbcTemplate.update(
                "INSERT INTO sanctions_list_version "
                        + "(version_id, source, downloaded_at, record_count, checksum, status) "
                        + "VALUES (?, ?, now(), ?, ?, 'ACTIVE')",
                versionId, "OFAC_SDN", 1, "checksum");
        jdbcTemplate.update(
                "INSERT INTO sanctioned_address (id, version_id, address, source) VALUES (?, ?, ?, ?)",
                UuidCreator.getTimeOrderedEpoch(), versionId, address, "OFAC_SDN");
        jdbcTemplate.update("DELETE FROM current_list_version WHERE id = 1");
        jdbcTemplate.update(
                "INSERT INTO current_list_version (id, version_id, updated_at) VALUES (1, ?, now())",
                versionId);
        return versionId;
    }
}

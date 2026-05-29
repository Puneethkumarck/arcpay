package com.arcpay.compliance.domain.service;

import com.arcpay.compliance.domain.model.CheckResult;
import com.arcpay.compliance.domain.model.CheckType;
import com.arcpay.compliance.domain.model.ScreeningCheck;
import com.arcpay.compliance.domain.model.ScreeningResult;
import com.arcpay.compliance.domain.model.ScreeningThreshold;
import com.arcpay.compliance.domain.model.Verdict;
import com.arcpay.compliance.domain.port.RiskSignalProvider;
import com.arcpay.compliance.domain.port.SanctionsSetProvider;
import com.arcpay.compliance.domain.port.ScreeningEngine;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
class ScreeningService implements ScreeningEngine {

    private static final int MAX_RISK_SCORE = 100;

    private final SanctionsSetProvider sanctionsSetProvider;
    private final List<RiskSignalProvider> riskSignalProviders;
    private final int holdThreshold;

    ScreeningService(
            SanctionsSetProvider sanctionsSetProvider,
            List<RiskSignalProvider> riskSignalProviders,
            ScreeningThreshold screeningThreshold) {
        this.sanctionsSetProvider = sanctionsSetProvider;
        this.riskSignalProviders = List.copyOf(riskSignalProviders);
        this.holdThreshold = screeningThreshold.holdThreshold();
    }

    @Override
    public ScreeningResult screen(UUID paymentId, UUID agentId, String recipientAddress) {
        var start = System.nanoTime();
        var normalized = AddressNormalizer.normalize(recipientAddress);
        var sanctionsSet = sanctionsSetProvider.getCurrentSanctionsSet();
        var listVersionId = sanctionsSet == null ? null : sanctionsSet.versionId();

        if (sanctionsSet != null && sanctionsSet.contains(normalized)) {
            log.info("Sanctions match for paymentId={} recipient={}", paymentId, normalized);
            return assemble(paymentId, agentId, normalized, Verdict.BLOCK, MAX_RISK_SCORE,
                    List.of(sanctionsMatchCheck(listVersionId)), listVersionId, start);
        }

        var checks = new ArrayList<ScreeningCheck>();
        for (var provider : riskSignalProviders) {
            checks.add(provider.provideSignal(normalized));
        }

        var riskScore = cap(checks.stream().mapToInt(ScreeningCheck::matchScore).sum());
        var verdict = riskScore >= holdThreshold ? Verdict.HOLD : Verdict.PASS;
        return assemble(paymentId, agentId, normalized, verdict, riskScore, checks, listVersionId, start);
    }

    private ScreeningResult assemble(
            UUID paymentId,
            UUID agentId,
            String recipientAddress,
            Verdict verdict,
            int riskScore,
            List<ScreeningCheck> checks,
            UUID listVersionId,
            long startNanos) {
        return ScreeningResult.builder()
                .screeningId(UuidCreator.getTimeOrderedEpoch())
                .paymentId(paymentId)
                .agentId(agentId)
                .recipientAddress(recipientAddress)
                .verdict(verdict)
                .riskScore(riskScore)
                .checks(checks)
                .listVersionId(listVersionId)
                .screenedAt(Instant.now())
                .durationMs((System.nanoTime() - startNanos) / 1_000_000)
                .build();
    }

    private static ScreeningCheck sanctionsMatchCheck(UUID listVersionId) {
        return ScreeningCheck.builder()
                .type(CheckType.SANCTIONS_OFAC)
                .result(CheckResult.MATCH)
                .matchScore(MAX_RISK_SCORE)
                .details(Map.of("listVersionId", String.valueOf(listVersionId)))
                .build();
    }

    private static int cap(int score) {
        return Math.min(MAX_RISK_SCORE, Math.max(0, score));
    }
}

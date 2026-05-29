package com.arcpay.compliance.fixtures;

import com.arcpay.compliance.domain.model.CheckResult;
import com.arcpay.compliance.domain.model.CheckType;
import com.arcpay.compliance.domain.model.HoldReview;
import com.arcpay.compliance.domain.model.ReviewState;
import com.arcpay.compliance.domain.model.SanctionsSet;
import com.arcpay.compliance.domain.model.ScreeningCheck;
import com.arcpay.compliance.domain.model.ScreeningResult;
import com.arcpay.compliance.domain.model.Verdict;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ComplianceFixtures {

    public static final UUID SOME_SCREENING_ID = UUID.fromString("0197aa00-1111-7def-8000-111111111111");
    public static final UUID SOME_PAYMENT_ID = UUID.fromString("0197aa00-2222-7def-8000-222222222222");
    public static final UUID SOME_AGENT_ID = UUID.fromString("0197aa00-3333-7def-8000-333333333333");
    public static final UUID SOME_REVIEW_ID = UUID.fromString("0197aa00-4444-7def-8000-444444444444");
    public static final UUID SOME_LIST_VERSION_ID = UUID.fromString("0197aa00-5555-7def-8000-555555555555");

    public static final String SOME_RECIPIENT_ADDRESS = "0xabcdef1234567890abcdef1234567890abcdef12";
    public static final String SOME_SANCTIONED_ADDRESS = "0x1111111111111111111111111111111111111111";
    public static final String SOME_WATCHLIST_ADDRESS = "0xabcabcabcabcabcabcabcabcabcabcabcabcabca";
    public static final String SOME_WATCHLIST_ADDRESS_MIXED_CASE = "0xABCabcABCabcABCabcABCabcABCabcABCabcABCa";
    public static final String SOME_WATCHLIST_LABEL = "operator-flagged mixer";
    public static final String SOME_MALFORMED_ADDRESS = "not-an-address";
    public static final String SOME_BLANK_ADDRESS = "   ";

    public static final Instant SOME_SCREENED_AT = Instant.parse("2026-06-01T10:05:00Z");
    public static final Instant SOME_CREATED_AT = Instant.parse("2026-06-01T10:05:00Z");

    public static final String SOME_REVIEWER_PRINCIPAL = "officer@arcpay.io";
    public static final String SOME_REVIEWER_ROLE = "COMPLIANCE_OFFICER";
    public static final String SOME_DECISION_REASON = "Verified counterparty via off-platform KYC; legitimate vendor.";

    public static final ScreeningCheck SOME_CLEAR_CHECK = ScreeningCheck.builder()
            .type(CheckType.SANCTIONS_OFAC)
            .result(CheckResult.CLEAR)
            .matchScore(0)
            .details(Map.of("source", "OFAC_SDN"))
            .build();

    public static final ScreeningCheck SOME_WATCHLIST_MATCH_CHECK = ScreeningCheck.builder()
            .type(CheckType.WATCHLIST)
            .result(CheckResult.FLAGGED)
            .matchScore(100)
            .details(Map.of("label", "operator-flagged"))
            .build();

    public static final ScreeningResult SOME_SCREENING_RESULT_PASS = ScreeningResult.builder()
            .screeningId(SOME_SCREENING_ID)
            .paymentId(SOME_PAYMENT_ID)
            .agentId(SOME_AGENT_ID)
            .recipientAddress(SOME_RECIPIENT_ADDRESS)
            .verdict(Verdict.PASS)
            .riskScore(0)
            .checks(List.of(SOME_CLEAR_CHECK))
            .listVersionId(SOME_LIST_VERSION_ID)
            .screenedAt(SOME_SCREENED_AT)
            .durationMs(42L)
            .build();

    public static final ScreeningResult SOME_SCREENING_RESULT_HOLD = ScreeningResult.builder()
            .screeningId(SOME_SCREENING_ID)
            .paymentId(SOME_PAYMENT_ID)
            .agentId(SOME_AGENT_ID)
            .recipientAddress(SOME_RECIPIENT_ADDRESS)
            .verdict(Verdict.HOLD)
            .riskScore(100)
            .checks(List.of(SOME_WATCHLIST_MATCH_CHECK))
            .listVersionId(SOME_LIST_VERSION_ID)
            .screenedAt(SOME_SCREENED_AT)
            .durationMs(58L)
            .build();

    public static final HoldReview SOME_HOLD_REVIEW_PENDING = HoldReview.builder()
            .reviewId(SOME_REVIEW_ID)
            .screeningId(SOME_SCREENING_ID)
            .paymentId(SOME_PAYMENT_ID)
            .agentId(SOME_AGENT_ID)
            .state(ReviewState.PENDING)
            .reviewerPrincipal(null)
            .reviewerRole(null)
            .reason(null)
            .createdAt(SOME_CREATED_AT)
            .decidedAt(null)
            .build();

    public static final SanctionsSet SOME_SANCTIONS_SET = SanctionsSet.builder()
            .versionId(SOME_LIST_VERSION_ID)
            .addresses(Set.of(SOME_SANCTIONED_ADDRESS))
            .loadedAt(SOME_SCREENED_AT)
            .build();

    private ComplianceFixtures() {}
}

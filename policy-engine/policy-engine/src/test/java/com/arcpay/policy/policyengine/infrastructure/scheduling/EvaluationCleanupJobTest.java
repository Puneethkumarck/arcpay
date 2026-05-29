package com.arcpay.policy.policyengine.infrastructure.scheduling;

import com.arcpay.policy.policyengine.domain.port.PolicyEvaluationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class EvaluationCleanupJobTest {

    private static final Instant SOME_NOW = Instant.parse("2026-05-29T02:00:00Z");
    private static final long SOME_RETENTION_DAYS = 90;

    @Mock
    private PolicyEvaluationRepository evaluationRepository;

    @Captor
    private ArgumentCaptor<Instant> cutoffCaptor;

    private EvaluationCleanupJob job;

    @BeforeEach
    void setUp() {
        job = new EvaluationCleanupJob(evaluationRepository, Clock.fixed(SOME_NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(job, "retentionDays", SOME_RETENTION_DAYS);
    }

    @Test
    void shouldDeleteEvaluationsOlderThan90Days() {
        // given
        var expectedCutoff = SOME_NOW.minus(Duration.ofDays(SOME_RETENTION_DAYS));

        // when
        job.cleanupOldEvaluations();

        // then
        then(evaluationRepository).should().deleteOlderThan(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isEqualTo(expectedCutoff);
    }

    @Test
    void shouldComputeCutoffFromConfiguredRetentionPeriod() {
        // given
        ReflectionTestUtils.setField(job, "retentionDays", 30L);
        var expectedCutoff = SOME_NOW.minus(Duration.ofDays(30));

        // when
        job.cleanupOldEvaluations();

        // then
        then(evaluationRepository).should().deleteOlderThan(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isEqualTo(expectedCutoff);
    }
}

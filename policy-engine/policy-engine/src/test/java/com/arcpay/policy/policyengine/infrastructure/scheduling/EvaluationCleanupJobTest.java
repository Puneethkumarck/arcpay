package com.arcpay.policy.policyengine.infrastructure.scheduling;

import com.arcpay.policy.policyengine.domain.port.PolicyEvaluationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class EvaluationCleanupJobTest {

    private static final Instant SOME_NOW = Instant.parse("2026-05-29T02:00:00Z");
    private static final long SOME_RETENTION_DAYS = 90;
    private static final Clock SOME_CLOCK = Clock.fixed(SOME_NOW, ZoneOffset.UTC);

    @Mock
    private PolicyEvaluationRepository evaluationRepository;

    @Captor
    private ArgumentCaptor<Instant> cutoffCaptor;

    @Test
    void shouldDeleteEvaluationsOlderThanConfiguredRetention() {
        // given
        var job = new EvaluationCleanupJob(evaluationRepository, SOME_CLOCK, SOME_RETENTION_DAYS);
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
        var job = new EvaluationCleanupJob(evaluationRepository, SOME_CLOCK, 30L);
        var expectedCutoff = SOME_NOW.minus(Duration.ofDays(30));

        // when
        job.cleanupOldEvaluations();

        // then
        then(evaluationRepository).should().deleteOlderThan(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isEqualTo(expectedCutoff);
    }

    @Test
    void shouldPropagateExceptionWhenDeleteFails() {
        // given
        var job = new EvaluationCleanupJob(evaluationRepository, SOME_CLOCK, SOME_RETENTION_DAYS);
        var expectedCutoff = SOME_NOW.minus(Duration.ofDays(SOME_RETENTION_DAYS));
        var failure = new IllegalStateException("db unavailable");
        willThrow(failure).given(evaluationRepository).deleteOlderThan(expectedCutoff);

        // when / then
        assertThatThrownBy(job::cleanupOldEvaluations).isSameAs(failure);
        then(evaluationRepository).should().deleteOlderThan(expectedCutoff);
    }
}

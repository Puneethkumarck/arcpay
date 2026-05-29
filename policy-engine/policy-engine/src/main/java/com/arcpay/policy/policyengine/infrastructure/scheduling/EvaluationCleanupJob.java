package com.arcpay.policy.policyengine.infrastructure.scheduling;

import com.arcpay.policy.policyengine.domain.port.PolicyEvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationCleanupJob {

    private final PolicyEvaluationRepository evaluationRepository;
    private final Clock clock;

    @Value("${arcpay.policy.evaluation.retention-days:90}")
    private long retentionDays;

    @Scheduled(cron = "${arcpay.policy.evaluation.cleanup-cron:0 0 2 * * *}")
    @SchedulerLock(name = "evaluationCleanup", lockAtMostFor = "30m", lockAtLeastFor = "5m")
    public void cleanupOldEvaluations() {
        var cutoff = Instant.now(clock).minus(Duration.ofDays(retentionDays));
        log.info("Deleting policy evaluations older than {} ({} day retention)", cutoff, retentionDays);
        evaluationRepository.deleteOlderThan(cutoff);
    }
}

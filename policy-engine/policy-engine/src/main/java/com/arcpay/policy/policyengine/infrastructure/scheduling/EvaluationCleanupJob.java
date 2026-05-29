package com.arcpay.policy.policyengine.infrastructure.scheduling;

import com.arcpay.policy.policyengine.domain.port.PolicyEvaluationRepository;
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
public class EvaluationCleanupJob {

    private final PolicyEvaluationRepository evaluationRepository;
    private final Clock clock;
    private final long retentionDays;

    public EvaluationCleanupJob(
            PolicyEvaluationRepository evaluationRepository,
            Clock clock,
            @Value("${arcpay.policy.evaluation.retention-days:90}") long retentionDays) {
        this.evaluationRepository = evaluationRepository;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${arcpay.policy.evaluation.cleanup-cron:0 0 2 * * *}")
    @SchedulerLock(name = "evaluationCleanup", lockAtMostFor = "30m", lockAtLeastFor = "5m")
    public void cleanupOldEvaluations() {
        var cutoff = Instant.now(clock).minus(Duration.ofDays(retentionDays));
        log.info("Deleting policy evaluations older than {} ({} day retention)", cutoff, retentionDays);
        try {
            evaluationRepository.deleteOlderThan(cutoff);
        } catch (RuntimeException e) {
            log.error("Failed to delete policy evaluations older than {} ({} day retention)", cutoff, retentionDays, e);
            throw e;
        }
    }
}

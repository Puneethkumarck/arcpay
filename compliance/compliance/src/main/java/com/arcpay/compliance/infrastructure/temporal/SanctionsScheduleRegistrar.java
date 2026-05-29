package com.arcpay.compliance.infrastructure.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.schedules.Schedule;
import io.temporal.client.schedules.ScheduleActionStartWorkflow;
import io.temporal.client.schedules.ScheduleClient;
import io.temporal.client.schedules.ScheduleException;
import io.temporal.client.schedules.ScheduleOptions;
import io.temporal.client.schedules.SchedulePolicy;
import io.temporal.client.schedules.ScheduleSpec;
import io.temporal.client.schedules.ScheduleState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.temporal.api.enums.v1.ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_SKIP;

@Slf4j
@Component
@RequiredArgsConstructor
class SanctionsScheduleRegistrar implements InitializingBean {

    static final String SCHEDULE_ID = "sanctions-ingestion-6h";

    private final WorkflowClient workflowClient;
    private final SanctionsIngestionProperties properties;

    @Override
    public void afterPropertiesSet() {
        var scheduleClient = ScheduleClient.newInstance(workflowClient.getWorkflowServiceStubs());
        try {
            scheduleClient.createSchedule(SCHEDULE_ID, buildSchedule(), ScheduleOptions.newBuilder().build());
            log.info("Created sanctions ingestion schedule {} with cron {}", SCHEDULE_ID, properties.refreshCron());
        } catch (ScheduleException e) {
            log.info("Sanctions ingestion schedule {} already registered; skipping creation", SCHEDULE_ID);
        }
    }

    private Schedule buildSchedule() {
        return Schedule.newBuilder()
                .setAction(ScheduleActionStartWorkflow.newBuilder()
                        .setWorkflowType(SanctionsIngestionWorkflow.class)
                        .setArguments("scheduled")
                        .setOptions(WorkflowOptions.newBuilder()
                                .setWorkflowId(SanctionsIngestionWorkflow.WORKFLOW_ID)
                                .setTaskQueue(SanctionsIngestionWorkflow.TASK_QUEUE)
                                .build())
                        .build())
                .setSpec(ScheduleSpec.newBuilder()
                        .setCronExpressions(List.of(properties.refreshCron()))
                        .build())
                .setPolicy(SchedulePolicy.newBuilder()
                        .setOverlap(SCHEDULE_OVERLAP_POLICY_SKIP)
                        .build())
                .setState(ScheduleState.newBuilder()
                        .setPaused(false)
                        .build())
                .build();
    }
}

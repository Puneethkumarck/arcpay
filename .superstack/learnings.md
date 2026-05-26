# Project Learnings

> Managed by `/learn`. Append-only — latest entry wins on conflicts.

## Patterns

### temporal-workflow-no-component
- **Insight:** Temporal `@WorkflowImpl` classes must NOT have `@Component` — Temporal manages their lifecycle; only `@ActivityImpl` classes get `@Component`
- **Confidence:** 9/10
- **Source:** manual
- **Files:** agent-identity-service/src/main/java/com/arcpay/identity/agentidentity/infrastructure/temporal/AgentProvisioningWorkflowImpl.java
- **Date:** 2026-05-26

### temporal-fail-compensation-as-activity
- **Insight:** Workflows can't call Spring beans directly; failure compensation (e.g., `failProvisioning`) must be routed through a separate non-retrying activity stub
- **Confidence:** 9/10
- **Source:** manual
- **Files:** agent-identity-service/src/main/java/com/arcpay/identity/agentidentity/infrastructure/temporal/AgentProvisioningWorkflowImpl.java
- **Date:** 2026-05-26

### temporal-workflow-logging
- **Insight:** Use `Workflow.getLogger()` inside workflow implementations, not Lombok `@Slf4j` — Temporal replays workflows and needs deterministic logging
- **Confidence:** 9/10
- **Source:** manual
- **Files:** agent-identity-service/src/main/java/com/arcpay/identity/agentidentity/infrastructure/temporal/AgentProvisioningWorkflowImpl.java
- **Date:** 2026-05-26

### kafka-consumer-idempotency-via-workflow-id
- **Insight:** Kafka consumer idempotency for Temporal workflows is achieved by using a deterministic workflow ID (`AgentProvisioning_{agentId}`) and catching `WorkflowExecutionAlreadyStarted`
- **Confidence:** 9/10
- **Source:** manual
- **Files:** agent-identity-service/src/main/java/com/arcpay/identity/agentidentity/application/stream/AgentProvisioningTrigger.java
- **Date:** 2026-05-26

## Pitfalls

### temporal-test-db-pollution
- **Insight:** Temporal workflow integration tests commit data outside `@Transactional` boundaries (workflows run in separate threads); must add explicit `@BeforeEach`/`@AfterEach` cleanup with FK-aware delete order (agents → owners)
- **Confidence:** 10/10
- **Source:** manual
- **Files:** agent-identity-service/src/integration-test/java/com/arcpay/identity/agentidentity/infrastructure/temporal/AgentProvisioningWorkflowIntegrationTest.java
- **Date:** 2026-05-26

### fk-aware-test-cleanup-order
- **Insight:** When tests clean up with `DELETE FROM`, respect FK order: delete child tables (agents) before parent tables (owners); forgetting this causes `DataIntegrityViolationException` in subsequent tests
- **Confidence:** 10/10
- **Source:** manual
- **Files:** agent-identity-service/src/integration-test/java/com/arcpay/identity/agentidentity/infrastructure/db/idempotency/IdempotencyKeyJpaRepositoryIntegrationTest.java
- **Date:** 2026-05-26

### kafka-serializer-stream-test-binder-conflict
- **Insight:** Spring Cloud Stream test binder overrides Kafka serializers to `ByteArraySerializer`; integration tests need explicit `spring.kafka.producer.key-serializer` and `value-serializer` in application-test.yml
- **Confidence:** 9/10
- **Source:** manual
- **Files:** agent-identity-service/src/integration-test/resources/application-test.yml
- **Date:** 2026-05-26

### temporal-sdk-api-version-mismatch
- **Insight:** Temporal SDK 1.35.0 uses `OnConflictOptions` instead of `WorkflowIdConflictPolicy` enum — always check the actual SDK version before using API features from docs/examples
- **Confidence:** 8/10
- **Source:** manual
- **Files:** agent-identity-service/src/main/java/com/arcpay/identity/agentidentity/application/stream/AgentProvisioningTrigger.java
- **Date:** 2026-05-26

## Preferences

### one-public-type-per-file
- **Insight:** Never nest public enums or records inside service classes; extract them to their own file in `domain/model/` — added to CLAUDE.md as a project rule
- **Confidence:** 10/10
- **Source:** manual
- **Files:** CLAUDE.md
- **Date:** 2026-05-26

## Architecture

### provisioning-saga-flow
- **Insight:** Agent provisioning saga: outbox event → Kafka (`agent.registration-requested`) → `AgentProvisioningTrigger` → Temporal `AgentProvisioningWorkflow` → `createCircleWallet` activity → `registerOnChain` activity → agent becomes ACTIVE
- **Confidence:** 10/10
- **Source:** manual
- **Files:** agent-identity-service/src/main/java/com/arcpay/identity/agentidentity/application/stream/AgentProvisioningTrigger.java, agent-identity-service/src/main/java/com/arcpay/identity/agentidentity/infrastructure/temporal/AgentProvisioningWorkflowImpl.java
- **Date:** 2026-05-26

### activity-loads-from-db-not-workflow-params
- **Insight:** Activities that need full entity data (e.g., `registerOnChain` needing `ownerId`, `metadataHash`, `policyHash`) should load from the DB rather than passing everything through workflow params — avoids stale data and keeps the workflow request lean
- **Confidence:** 8/10
- **Source:** manual
- **Files:** agent-identity-service/src/main/java/com/arcpay/identity/agentidentity/infrastructure/temporal/AgentProvisioningActivitiesImpl.java
- **Date:** 2026-05-26

## Tools

### temporal-test-server-spring-boot
- **Insight:** `spring.temporal.test-server.enabled: true` in application-test.yml auto-starts an in-process Temporal test server and registers all `@WorkflowImpl`/`@ActivityImpl` beans — no manual test environment setup needed
- **Confidence:** 9/10
- **Source:** manual
- **Files:** agent-identity-service/src/integration-test/resources/application-test.yml
- **Date:** 2026-05-26

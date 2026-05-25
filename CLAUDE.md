# CLAUDE.md

## Project Overview

ArcPay is an open-source payment protocol on Circle's Arc L1 blockchain giving AI agents autonomous but policy-controlled access to USDC. This repo contains the backend microservices: Agent Identity, Policy Engine, Compliance Shield, Payment Execution, and Audit Ledger.


**Current focus:** Agent Identity Service (first service being built).

## Tech Stack

- **Language:** Java 25 (use toolchain specification in Gradle)
- **Framework:** Spring Boot 4.x, Spring Cloud 2025.x
- **Build:** Gradle with Kotlin DSL, `io.spring.dependency-management` plugin, version catalog (`gradle/libs.versions.toml`)
- **Database:** PostgreSQL, Flyway migrations (convention: `V{N}__{TICKET}_{description}.sql`)
- **Messaging:** Kafka via Spring Cloud Stream, namastack transactional outbox
- **Orchestration:** Temporal (`io.temporal:temporal-spring-boot-starter`)
- **Blockchain:** web3j for Arc L1 smart contract interaction
- **Mapping:** MapStruct (`@Mapper(componentModel = "spring")`)
- **Boilerplate:** Lombok (`@RequiredArgsConstructor`, `@Builder`, `@Slf4j`, `@Getter`)

## Architecture

Hexagonal (Ports & Adapters) with strict dependency direction: `application -> domain <- infrastructure`.

```
com.arcpay.identity.agentidentity
  ├── domain/           # Business logic, models, ports — ZERO infra dependencies
  ├── application/      # REST controllers, Kafka listeners, security filters
  └── infrastructure/   # JPA entities, repository adapters, external clients
```

### Layer Rules

- **Domain** must NOT import from `application` or `infrastructure`
- **Domain** must NOT use JPA annotations (`@Entity`, `@Table`, `@Column`)
- **Domain** models are Java records with `@Builder(toBuilder = true)` — no Spring annotations on records
- **Domain** services use only `@Component`, `@Service`, `@Transactional` from Spring
- **Infrastructure** adapters that implement domain ports are **package-private** classes
- **Application** controllers use `@RestController` + `@Validated`

## Coding Conventions

### Dependency Injection
- Always `@RequiredArgsConstructor` with `private final` fields — never `@Autowired`

### Domain Models
- Java records with `@Builder(toBuilder = true)` per ADR-002
- State transitions return new instances (immutable): `agent.withWallet(...)` returns new Agent
- Domain exceptions are plain `RuntimeException` subclasses with contextual messages — NO error code field

### JPA Entities
- `@NoArgsConstructor` + `@AllArgsConstructor` + `@Getter` + `@Setter` + `@Builder(toBuilder = true)`
- `@ToString(onlyExplicitlyIncluded = true)` — include only PK fields
- `@JdbcTypeCode(VARCHAR)` on all UUID fields
- `@Enumerated(EnumType.STRING)` on enums — never ordinal

### Error Handling
- Error codes: `ARCPAY-IDENTITY-{NNNN}` format per ADR-008
- Error responses: `ApiError(code, status, message, details)` per ADR-009 — NOT `{timestamp, traceId}`
- Domain exceptions carry contextual info in message; error code mapping is `GlobalExceptionHandler`'s job

### Object Mapping (MapStruct)
- Entity mappers: `mapToEntity()` / `mapToDomain()`
- API mappers: `toApi()` / `toDomain()`

### Event Publishing
- Outbox publisher uses `@Transactional(propagation = MANDATORY)` — events MUST participate in caller's transaction
- `EventPublisher<T>` is a generic port interface in domain
- Each event record has `public static final String TOPIC`

### Temporal
- Auto-discovery via `@WorkflowImpl(taskQueues = ...)` + `@ActivityImpl(taskQueues = ...) + @Component`
- Workflow ID convention: `{WorkflowBaseName}_{businessId}` (e.g., `AgentProvisioning_{agentId}`)
- Permanent failures: `ApplicationFailure.newNonRetryableFailure()`

## Testing Standards

### Assertions
- **AssertJ exclusively** — no JUnit assertions
- **GOLDEN RULE:** Build expected object + single `usingRecursiveComparison()` — multiple `assertThat` calls on individual fields are FORBIDDEN

### Mocking
- **BDD Mockito only:** `given().willReturn()` — never `when().thenReturn()`
- Use `eqIgnoringTimestamps()` and `eqIgnoring()` from TestUtils — never `any()` or `eq()` matchers

### Naming
- Test methods: `should*` in camelCase (e.g., `shouldRegisterOwnerAndPublishEvent`)
- Fixtures: `SOME_*` prefix (e.g., `SOME_OWNER`, `SOME_AGENT_ACTIVE`)

### Test Types
| Type | Location | Base Class | Runs With |
|------|----------|------------|-----------|
| Unit | `src/test/` | `@ExtendWith(MockitoExtension.class)` | `./gradlew test` |
| Integration | `src/integration-test/` | `FullContextIntegrationTest` | `./gradlew integrationTest` |
| Business (E2E) | `src/business-test/` | `BusinessTest` | `./gradlew businessTest` |
| Architecture | `src/test/` | `@AnalyzeClasses` | `./gradlew test` |

### Fixtures
- Located in `src/testFixtures/`
- WireMock stubs in `stubs/` subpackage

## Standard Documentation

Authoritative references in `docs/standards/` (reading order):

1. **Foundation** (read before writing any code):
   - `tech-stack.md` — dependency versions and framework choices
   - `project-structure.md` — Gradle multi-module layout, package conventions
   - `coding-standards.md` — hexagonal architecture rules, domain modeling, API design
   - `testing-standards.md` — test strategy, naming, assertions, fixtures

2. **Reference** (consult for design decisions):
   - `adr.md` — 23 Architecture Decision Records with rationale
   - `backend-patterns-template.md` — Problem -> Solution cookbook (23 pattern areas)

3. **Integration** (load when feature requires it):
   - `temporal-patterns.md` — Temporal workflows, activities, signals, testing
   - `namastack-outbox.md` — transactional outbox with Kafka

## Key Docs

| Path | Purpose |
|------|---------|
| `docs/FUNCTIONAL_SPEC.md` | Full ArcPay functional specification (8 features, all services) |
| `docs/specs/agent-identity-service-spec.md` | Implementation-ready spec for Agent Identity Service |
| `docs/issues/agent-identity-service-issues.md` | 28 GitHub issues as vertical slices (source of truth for issue details) |
| `docs/ideas/agent-identity-service.md` | Refined idea doc for Agent Identity |

## Git & Workflow

- Branch naming: `feature/{issue-number}-{short-description}`
- Commit messages: concise, describe the "why"
- `docs/` is in `.gitignore` — internal planning docs are not committed

## Worktree Agents

The `docs/` directory is gitignored and won't exist in git worktrees. Worktree agents (e.g., Superset) must read docs from the main working tree using absolute paths:

- **Main working tree:** `/Users/puneethkumarck/Documents/AI/github/arcpay`
- **Standards:** `/Users/puneethkumarck/Documents/AI/github/arcpay/docs/standards/`
- **Spec:** `/Users/puneethkumarck/Documents/AI/github/arcpay/docs/specs/agent-identity-service-spec.md`
- **Issues:** `/Users/puneethkumarck/Documents/AI/github/arcpay/docs/issues/agent-identity-service-issues.md`

## Design Decisions (Quick Ref)

These are explicit choices made during planning — do not change without discussion:

- **Domain models as records** — not classes with StateMachine pattern (ADR-002 with deliberate override)
- **`@Transactional` directly on handlers** — not TransactionalProxy pattern (ADR-005 override)
- **Repository adapters are package-private** — only exposed through domain ports
- **PostgreSQL is source of truth** — on-chain AgentRegistry.sol is a verifiable projection
- **Event-sourced identity** — provisioning saga: DB save -> outbox event -> Kafka -> Temporal workflow


## Working principles
Avoid "not my department" thinking — if there are build failures you consider unrelated to current changes, still make an effort to fix them.

Never add Claude (or any Anthropic identity) as a Co-Authored-By trailer on commit messages. Human co-authors are fine.

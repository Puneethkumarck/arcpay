# Wave 2 — Superset Prompts (Issues #3, #4, #5, #6)

Paste each prompt into a separate Superset workspace. Each agent works independently in its own worktree.

---

## Workspace 1: Issue #3 — Owner Entity + JPA Repository + Repository Adapter

```
Implement GitHub issue #3 for the ArcPay Agent Identity Service: Owner Entity + JPA Repository + Repository Adapter.

READ FIRST:
- CLAUDE.md (project root) for all coding conventions
- docs/standards/coding-standards.md (absolute path: /Users/puneethkumarck/Documents/AI/github/arcpay/docs/standards/coding-standards.md)
- docs/standards/testing-standards.md (absolute path: /Users/puneethkumarck/Documents/AI/github/arcpay/docs/standards/testing-standards.md)
- docs/specs/agent-identity-service-spec.md §2 for Owner data model (absolute path: /Users/puneethkumarck/Documents/AI/github/arcpay/docs/specs/agent-identity-service-spec.md)
- The docs/ directory is gitignored and won't exist in worktrees — read from the absolute paths above.

BRANCH: feature/3-owner-entity-jpa-repository

EXISTING CODE TO REFERENCE:
- FlywayMigrationIntegrationTest.java in src/integration-test/ — shows the testing pattern
- FullContextIntegrationTest.java in src/testFixtures/ — base class for all integration tests
- TestContainerSupport.java in src/testFixtures/ — PostgreSQL container helper
- V1__create_owners_table.sql — the DB schema this entity maps to

WHAT TO BUILD:

1. Domain model (src/main/java/com/arcpay/identity/agentidentity/domain/model/):
   - Owner.java — Java record with @Builder(toBuilder = true). Fields: ownerId (UUID), email (String), walletAddress (String), apiKeyHash (String), status (OwnerStatus), createdAt (Instant), updatedAt (Instant)
   - OwnerStatus.java — enum: ACTIVE, SUSPENDED

2. Domain port (src/main/java/com/arcpay/identity/agentidentity/domain/port/):
   - OwnerRepository.java — interface with: save(Owner), findById(UUID): Optional<Owner>, findByApiKeyHash(String): Optional<Owner>, existsByEmailIgnoreCase(String): boolean, existsByWalletAddressIgnoreCase(String): boolean

3. Infrastructure entity (src/main/java/com/arcpay/identity/agentidentity/infrastructure/db/owner/):
   - OwnerEntity.java — JPA entity with: @NoArgsConstructor @AllArgsConstructor @Getter @Setter @Builder(toBuilder = true), @ToString(onlyExplicitlyIncluded = true) with only ownerId included, @JdbcTypeCode(VARCHAR) on UUID fields, @Enumerated(EnumType.STRING) on status
   - OwnerJpaRepository.java — Spring Data interface: findByApiKeyHash, existsByEmailIgnoreCase, existsByWalletAddressIgnoreCase
   - OwnerRepositoryAdapter.java — PACKAGE-PRIVATE class (no public modifier), @Component @RequiredArgsConstructor, implements OwnerRepository port, maps via OwnerEntityMapper
   - mapper/OwnerEntityMapper.java — @Mapper(componentModel = "spring"), methods: mapToEntity(Owner), mapToDomain(OwnerEntity)

4. Test fixtures (src/testFixtures/java/com/arcpay/identity/agentidentity/fixtures/):
   - OwnerFixtures.java — public final class, private constructor, SOME_OWNER (domain model), SOME_OWNER_ENTITY (JPA entity), SOME_OWNER_ID, SOME_EMAIL, SOME_WALLET_ADDRESS, SOME_API_KEY_HASH constants

5. Unit tests (src/test/java/):
   - OwnerEntityMapperTest.java — @ExtendWith(MockitoExtension.class), test mapToEntity and mapToDomain using Mappers.getMapper(), single usingRecursiveComparison() assertion

6. Integration tests (src/integration-test/java/):
   - OwnerJpaRepositoryIntegrationTest.java — extends FullContextIntegrationTest, tests: shouldFindOwnerByApiKeyHash, shouldReturnTrueForExistingEmailCaseInsensitive, shouldReturnTrueForExistingWalletAddressCaseInsensitive, shouldRejectDuplicateEmail
   - OwnerRepositoryAdapterIntegrationTest.java — extends FullContextIntegrationTest, round-trip save/find with usingRecursiveComparison().ignoringFields("createdAt", "updatedAt")

TESTING RULES:
- AssertJ only, BDD Mockito (given/willReturn, then/should — NEVER when/thenReturn or verify)
- Golden rule: build expected object + single usingRecursiveComparison() — never multiple assertThat on individual fields
- Use var for ALL local variables
- // given, // when, // then comment markers
- should* camelCase test method names
- @Autowired is fine for test classes (not production code)
- No comments or Javadoc beyond given/when/then

VERIFICATION:
- ./gradlew test — unit tests pass
- ./gradlew integrationTest — integration tests pass
- ./gradlew build — full build passes

After all tests pass, commit with message: "#3: Add Owner entity, JPA repository, and repository adapter"
Then push: git push -u origin feature/3-owner-entity-jpa-repository
```

---

## Workspace 2: Issue #4 — Agent Entity + JPA Repository + Repository Adapter

```
Implement GitHub issue #4 for the ArcPay Agent Identity Service: Agent Entity + JPA Repository + Repository Adapter.

READ FIRST:
- CLAUDE.md (project root) for all coding conventions
- docs/standards/coding-standards.md (absolute path: /Users/puneethkumarck/Documents/AI/github/arcpay/docs/standards/coding-standards.md)
- docs/standards/testing-standards.md (absolute path: /Users/puneethkumarck/Documents/AI/github/arcpay/docs/standards/testing-standards.md)
- docs/specs/agent-identity-service-spec.md §3 for Agent data model (absolute path: /Users/puneethkumarck/Documents/AI/github/arcpay/docs/specs/agent-identity-service-spec.md)
- The docs/ directory is gitignored and won't exist in worktrees — read from the absolute paths above.

BRANCH: feature/4-agent-entity-jpa-repository

EXISTING CODE TO REFERENCE:
- FlywayMigrationIntegrationTest.java in src/integration-test/ — shows the testing pattern
- FullContextIntegrationTest.java in src/testFixtures/ — base class for all integration tests
- V2__create_agents_table.sql — the DB schema this entity maps to
- V1__create_owners_table.sql — agents have FK to owners, tests need owner records

WHAT TO BUILD:

1. Domain model (src/main/java/com/arcpay/identity/agentidentity/domain/model/):
   - Agent.java — Java record with @Builder(toBuilder = true). Fields: agentId (UUID), ownerId (UUID), name (String), purpose (String), status (AgentStatus), walletId (String, nullable), walletAddress (String, nullable), onChainTxHash (String, nullable), policyHash (String, nullable), metadataHash (String), failureReason (String, nullable), createdAt (Instant), updatedAt (Instant). Include withWallet(), withOnChainRegistration(), withFailure() methods per spec §3.1.
   - AgentStatus.java — enum: PROVISIONING, WALLET_READY, ACTIVE, SUSPENDED, FAILED

2. Domain port (src/main/java/com/arcpay/identity/agentidentity/domain/port/):
   - AgentRepository.java — interface with: save(Agent), findById(UUID): Optional<Agent>, findByIdForUpdate(UUID): Optional<Agent>, findByOwnerIdAndStatus(UUID, AgentStatus, Pageable): Page<Agent>, findByOwnerId(UUID, Pageable): Page<Agent>, existsByOwnerIdAndNameIgnoreCase(UUID, String): boolean, existsByOwnerIdAndNameIgnoreCaseAndAgentIdNot(UUID, String, UUID): boolean

3. Infrastructure entity (src/main/java/com/arcpay/identity/agentidentity/infrastructure/db/agent/):
   - AgentEntity.java — JPA entity: @NoArgsConstructor @AllArgsConstructor @Getter @Setter @Builder(toBuilder = true), @ToString(onlyExplicitlyIncluded = true) with agentId and ownerId, @JdbcTypeCode(VARCHAR) on ALL UUID fields, @Enumerated(EnumType.STRING) on status
   - AgentJpaRepository.java — Spring Data interface: findByIdForUpdate with @Lock(PESSIMISTIC_WRITE) + @Query, findByOwnerIdAndStatus (paginated), findByOwnerId (paginated), existsByOwnerIdAndNameIgnoreCase, existsByOwnerIdAndNameIgnoreCaseAndAgentIdNot
   - AgentRepositoryAdapter.java — PACKAGE-PRIVATE class, @Component @RequiredArgsConstructor, implements AgentRepository port
   - mapper/AgentEntityMapper.java — @Mapper(componentModel = "spring"), mapToEntity(Agent), mapToDomain(AgentEntity)

4. You also need Owner domain model and entity for the FK relationship. Create minimal versions:
   - domain/model/Owner.java, domain/model/OwnerStatus.java (if they don't exist)
   - infrastructure/db/owner/OwnerEntity.java, OwnerJpaRepository.java (minimal, just enough for FK)
   - Note: Issue #3 is building the full Owner persistence layer in parallel. Keep your Owner files minimal — just enough to satisfy the agents FK constraint in tests.

5. Test fixtures (src/testFixtures/java/com/arcpay/identity/agentidentity/fixtures/):
   - AgentFixtures.java — SOME_AGENT_PROVISIONING, SOME_AGENT_ACTIVE, SOME_AGENT_SUSPENDED, SOME_AGENT_FAILED with realistic field values
   - OwnerFixtures.java (minimal) — SOME_OWNER_ID, SOME_OWNER_ENTITY for setting up FK prerequisite data

6. Unit tests (src/test/java/):
   - AgentEntityMapperTest.java — test all fields including nulls (walletId, walletAddress, onChainTxHash, policyHash, failureReason)

7. Integration tests (src/integration-test/java/):
   - AgentJpaRepositoryIntegrationTest.java — extends FullContextIntegrationTest. Insert owner first using JdbcTemplate or OwnerJpaRepository. Tests: shouldFindByIdForUpdateWithPessimisticLock, shouldPaginateByOwnerIdAndStatus, shouldDetectNameExistsCaseInsensitivePerOwner, shouldAllowSameNameForDifferentOwners, shouldRejectDuplicateNameForSameOwner
   - AgentRepositoryAdapterIntegrationTest.java — round-trip for all agent states

TESTING RULES:
- AssertJ only, BDD Mockito (given/willReturn, then/should — NEVER when/thenReturn or verify)
- Golden rule: build expected object + single usingRecursiveComparison() — never multiple assertThat on individual fields
- Use var for ALL local variables
- // given, // when, // then comment markers
- should* camelCase test method names
- @Autowired is fine for test classes (not production code)
- No comments or Javadoc beyond given/when/then

VERIFICATION:
- ./gradlew test — unit tests pass
- ./gradlew integrationTest — integration tests pass
- ./gradlew build — full build passes

After all tests pass, commit with message: "#4: Add Agent entity, JPA repository, and repository adapter"
Then push: git push -u origin feature/4-agent-entity-jpa-repository
```

---

## Workspace 3: Issue #5 — Idempotency Key Entity + Repository

```
Implement GitHub issue #5 for the ArcPay Agent Identity Service: Idempotency Key Entity + Repository.

READ FIRST:
- CLAUDE.md (project root) for all coding conventions
- docs/standards/coding-standards.md (absolute path: /Users/puneethkumarck/Documents/AI/github/arcpay/docs/standards/coding-standards.md)
- docs/standards/testing-standards.md (absolute path: /Users/puneethkumarck/Documents/AI/github/arcpay/docs/standards/testing-standards.md)
- docs/specs/agent-identity-service-spec.md (absolute path: /Users/puneethkumarck/Documents/AI/github/arcpay/docs/specs/agent-identity-service-spec.md)
- The docs/ directory is gitignored and won't exist in worktrees — read from the absolute paths above.

BRANCH: feature/5-idempotency-key-entity-repository

EXISTING CODE TO REFERENCE:
- FlywayMigrationIntegrationTest.java in src/integration-test/ — shows the testing pattern
- FullContextIntegrationTest.java in src/testFixtures/ — base class for all integration tests
- V3__create_idempotency_keys_table.sql — the DB schema: composite PK (idempotency_key, owner_id), FK to owners

WHAT TO BUILD:

1. Domain model (src/main/java/com/arcpay/identity/agentidentity/domain/model/):
   - IdempotencyKey.java — Java record with @Builder(toBuilder = true). Fields: idempotencyKey (UUID), ownerId (UUID), endpoint (String), responseStatus (int), responseBody (String), createdAt (Instant), expiresAt (Instant)

2. Domain port (src/main/java/com/arcpay/identity/agentidentity/domain/port/):
   - IdempotencyKeyRepository.java — interface: save(IdempotencyKey), findByKeyAndOwnerId(UUID key, UUID ownerId): Optional<IdempotencyKey>, deleteExpiredBefore(Instant cutoff)

3. Infrastructure entity (src/main/java/com/arcpay/identity/agentidentity/infrastructure/db/idempotency/):
   - IdempotencyKeyEntity.java — JPA entity with composite PK via @IdClass or @EmbeddedId. Fields: idempotencyKey (UUID), ownerId (UUID), endpoint, responseStatus, responseBody, createdAt, expiresAt. Use @JdbcTypeCode(VARCHAR) on UUID fields.
   - IdempotencyKeyId.java — composite key class (implements Serializable, equals/hashCode). Can use @Embeddable or plain class with @IdClass.
   - IdempotencyKeyJpaRepository.java — Spring Data interface: findByIdempotencyKeyAndOwnerId, deleteByExpiresAtBefore
   - IdempotencyKeyRepositoryAdapter.java — PACKAGE-PRIVATE class, @Component @RequiredArgsConstructor, implements IdempotencyKeyRepository port
   - mapper/IdempotencyKeyEntityMapper.java — @Mapper(componentModel = "spring"), mapToEntity/mapToDomain

4. You need a minimal Owner entity for the FK relationship in tests. Create:
   - infrastructure/db/owner/OwnerEntity.java (minimal, just ID + required fields)
   - Or use JdbcTemplate to insert owner rows directly in tests
   - Note: Issue #3 builds the full Owner layer in parallel. Keep your Owner usage minimal.

5. Test fixtures (src/testFixtures/java/com/arcpay/identity/agentidentity/fixtures/):
   - IdempotencyKeyFixtures.java — SOME_IDEMPOTENCY_KEY, SOME_IDEMPOTENCY_KEY_EXPIRED with realistic values

6. Integration tests (src/integration-test/java/.../infrastructure/db/idempotency/):
   - IdempotencyKeyJpaRepositoryIntegrationTest.java — extends FullContextIntegrationTest. Insert owner first (FK constraint). Tests:
     - shouldSaveAndFindByCompositeKey
     - shouldTreatSameKeyForDifferentOwnersAsSeparate
     - shouldDeleteExpiredEntries
     - shouldReturnEmptyForNonExistentKey

TESTING RULES:
- AssertJ only, BDD Mockito (given/willReturn, then/should — NEVER when/thenReturn or verify)
- Golden rule: build expected object + single usingRecursiveComparison() — never multiple assertThat on individual fields
- Use var for ALL local variables
- // given, // when, // then comment markers
- should* camelCase test method names
- @Autowired is fine for test classes (not production code)
- No comments or Javadoc beyond given/when/then

VERIFICATION:
- ./gradlew test — unit tests pass (if any)
- ./gradlew integrationTest — integration tests pass
- ./gradlew build — full build passes

After all tests pass, commit with message: "#5: Add IdempotencyKey entity, JPA repository, and repository adapter"
Then push: git push -u origin feature/5-idempotency-key-entity-repository
```

---

## Workspace 4: Issue #6 — Gas Usage Entity + Repository

```
Implement GitHub issue #6 for the ArcPay Agent Identity Service: Gas Usage Entity + Repository.

READ FIRST:
- CLAUDE.md (project root) for all coding conventions
- docs/standards/coding-standards.md (absolute path: /Users/puneethkumarck/Documents/AI/github/arcpay/docs/standards/coding-standards.md)
- docs/standards/testing-standards.md (absolute path: /Users/puneethkumarck/Documents/AI/github/arcpay/docs/standards/testing-standards.md)
- docs/specs/agent-identity-service-spec.md (absolute path: /Users/puneethkumarck/Documents/AI/github/arcpay/docs/specs/agent-identity-service-spec.md)
- The docs/ directory is gitignored and won't exist in worktrees — read from the absolute paths above.

BRANCH: feature/6-gas-usage-entity-repository

EXISTING CODE TO REFERENCE:
- FlywayMigrationIntegrationTest.java in src/integration-test/ — shows the testing pattern
- FullContextIntegrationTest.java in src/testFixtures/ — base class for all integration tests
- V5__create_gas_usage_table.sql — the DB schema: id UUID PK, owner_id FK, agent_id nullable, operation, tx_hash, gas_used BIGINT, gas_cost_usdc NUMERIC(18,8), created_at

WHAT TO BUILD:

1. Domain model (src/main/java/com/arcpay/identity/agentidentity/domain/model/):
   - GasUsage.java — Java record with @Builder(toBuilder = true). Fields: id (UUID), ownerId (UUID), agentId (UUID, nullable), operation (String), txHash (String), gasUsed (long), gasCostUsdc (BigDecimal), createdAt (Instant)

2. Domain port (src/main/java/com/arcpay/identity/agentidentity/domain/port/):
   - GasUsageRepository.java — interface: save(GasUsage), findByOwnerId(UUID ownerId, Pageable pageable): Page<GasUsage>

3. Infrastructure entity (src/main/java/com/arcpay/identity/agentidentity/infrastructure/db/gasusage/):
   - GasUsageEntity.java — JPA entity: @NoArgsConstructor @AllArgsConstructor @Getter @Setter @Builder(toBuilder = true), @ToString(onlyExplicitlyIncluded = true) with only id, @JdbcTypeCode(VARCHAR) on UUID fields, BigDecimal for gasCostUsdc with @Column(precision = 18, scale = 8)
   - GasUsageJpaRepository.java — Spring Data interface: findByOwnerId(UUID, Pageable): Page<GasUsageEntity>
   - GasUsageRepositoryAdapter.java — PACKAGE-PRIVATE class, @Component @RequiredArgsConstructor, implements GasUsageRepository port
   - mapper/GasUsageEntityMapper.java — @Mapper(componentModel = "spring"), mapToEntity(GasUsage), mapToDomain(GasUsageEntity)

4. You need a minimal Owner entity for the FK relationship in tests. Create:
   - infrastructure/db/owner/OwnerEntity.java (minimal, just ID + required fields)
   - Or use JdbcTemplate to insert owner rows directly in tests
   - Note: Issue #3 builds the full Owner layer in parallel. Keep your Owner usage minimal.

5. Test fixtures (src/testFixtures/java/com/arcpay/identity/agentidentity/fixtures/):
   - GasUsageFixtures.java — SOME_GAS_USAGE, SOME_GAS_USAGE_WITHOUT_AGENT with realistic values (operation like "REGISTER_AGENT", txHash, gasUsed, gasCostUsdc with proper precision)

6. Unit tests (src/test/java/):
   - GasUsageEntityMapperTest.java — test mapToEntity and mapToDomain, verify BigDecimal precision is preserved, verify nullable agentId handled correctly

7. Integration tests (src/integration-test/java/.../infrastructure/db/gasusage/):
   - GasUsageJpaRepositoryIntegrationTest.java — extends FullContextIntegrationTest. Insert owner first (FK constraint). Tests:
     - shouldSaveAndQueryByOwner (with pagination)
     - shouldHandleNullableAgentId
     - shouldPreserveBigDecimalPrecision (save 0.00123456 and verify exact value returned)

TESTING RULES:
- AssertJ only, BDD Mockito (given/willReturn, then/should — NEVER when/thenReturn or verify)
- Golden rule: build expected object + single usingRecursiveComparison() — never multiple assertThat on individual fields
- Use var for ALL local variables
- // given, // when, // then comment markers
- should* camelCase test method names
- @Autowired is fine for test classes (not production code)
- No comments or Javadoc beyond given/when/then

VERIFICATION:
- ./gradlew test — unit tests pass
- ./gradlew integrationTest — integration tests pass
- ./gradlew build — full build passes

After all tests pass, commit with message: "#6: Add GasUsage entity, JPA repository, and repository adapter"
Then push: git push -u origin feature/6-gas-usage-entity-repository
```

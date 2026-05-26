package com.arcpay.identity.agentidentity.fixtures;

import com.arcpay.identity.agentidentity.domain.model.IdempotencyKey;
import com.arcpay.identity.agentidentity.infrastructure.db.idempotency.IdempotencyKeyEntity;

import java.time.Instant;
import java.util.UUID;

public final class IdempotencyKeyFixtures {

    public static final UUID SOME_IDEMPOTENCY_KEY_ID = UUID.fromString("0197a000-1111-7abc-8000-000000000001");
    public static final UUID SOME_EXPIRED_IDEMPOTENCY_KEY_ID = UUID.fromString("0197a000-2222-7abc-8000-000000000002");
    public static final UUID SOME_OWNER_ID = UUID.fromString("019718a0-1234-7def-8000-abcdef123456");

    public static final String SOME_ENDPOINT = "POST /v1/agents";
    public static final String SOME_RESPONSE_BODY = "{\"agentId\":\"019718a0-aaaa-7def-8000-000000000001\",\"status\":\"PROVISIONING\"}";
    public static final Instant SOME_CREATED_AT = Instant.parse("2026-06-01T10:00:00Z");
    public static final Instant SOME_EXPIRES_AT = Instant.parse("2026-06-02T10:00:00Z");
    public static final Instant SOME_EXPIRED_CREATED_AT = Instant.parse("2026-05-29T10:00:00Z");
    public static final Instant SOME_EXPIRED_EXPIRES_AT = Instant.parse("2026-05-30T10:00:00Z");

    public static final IdempotencyKey SOME_IDEMPOTENCY_KEY = IdempotencyKey.builder()
            .idempotencyKey(SOME_IDEMPOTENCY_KEY_ID)
            .ownerId(SOME_OWNER_ID)
            .endpoint(SOME_ENDPOINT)
            .responseStatus(201)
            .responseBody(SOME_RESPONSE_BODY)
            .createdAt(SOME_CREATED_AT)
            .expiresAt(SOME_EXPIRES_AT)
            .build();

    public static final IdempotencyKey SOME_IDEMPOTENCY_KEY_EXPIRED = IdempotencyKey.builder()
            .idempotencyKey(SOME_EXPIRED_IDEMPOTENCY_KEY_ID)
            .ownerId(SOME_OWNER_ID)
            .endpoint(SOME_ENDPOINT)
            .responseStatus(201)
            .responseBody(SOME_RESPONSE_BODY)
            .createdAt(SOME_EXPIRED_CREATED_AT)
            .expiresAt(SOME_EXPIRED_EXPIRES_AT)
            .build();

    public static final IdempotencyKeyEntity SOME_IDEMPOTENCY_KEY_ENTITY = IdempotencyKeyEntity.builder()
            .idempotencyKey(SOME_IDEMPOTENCY_KEY_ID)
            .ownerId(SOME_OWNER_ID)
            .endpoint(SOME_ENDPOINT)
            .responseStatus(201)
            .responseBody(SOME_RESPONSE_BODY)
            .createdAt(SOME_CREATED_AT)
            .expiresAt(SOME_EXPIRES_AT)
            .build();

    public static final IdempotencyKeyEntity SOME_IDEMPOTENCY_KEY_ENTITY_EXPIRED = IdempotencyKeyEntity.builder()
            .idempotencyKey(SOME_EXPIRED_IDEMPOTENCY_KEY_ID)
            .ownerId(SOME_OWNER_ID)
            .endpoint(SOME_ENDPOINT)
            .responseStatus(201)
            .responseBody(SOME_RESPONSE_BODY)
            .createdAt(SOME_EXPIRED_CREATED_AT)
            .expiresAt(SOME_EXPIRED_EXPIRES_AT)
            .build();

    private IdempotencyKeyFixtures() {
    }
}

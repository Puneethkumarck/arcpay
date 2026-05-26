package com.arcpay.identity.agentidentity.application.controller.agent.handler;

import com.arcpay.identity.agentidentity.domain.exception.MissingIdempotencyKeyException;
import com.arcpay.identity.agentidentity.domain.model.IdempotencyKey;
import com.arcpay.identity.agentidentity.domain.port.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyHandler {

    private static final long IDEMPOTENCY_KEY_TTL_HOURS = 24;

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final JsonMapper jsonMapper;

    @Transactional
    public <T> ResponseEntity<T> handle(String idempotencyKeyHeader, UUID ownerId, String endpoint,
                                        Supplier<T> action, Class<T> responseType) {
        if (idempotencyKeyHeader == null || idempotencyKeyHeader.isBlank()) {
            throw new MissingIdempotencyKeyException();
        }

        final UUID idempotencyKey;
        try {
            idempotencyKey = UUID.fromString(idempotencyKeyHeader);
        } catch (IllegalArgumentException ex) {
            throw new MissingIdempotencyKeyException();
        }

        var existing = idempotencyKeyRepository.findByKeyAndOwnerId(idempotencyKey, ownerId);
        if (existing.isPresent()) {
            return toResponse(existing.get(), responseType);
        }

        var result = action.get();
        var now = Instant.now();
        saveIdempotencyKey(idempotencyKey, ownerId, endpoint, HttpStatus.CREATED.value(), result, now);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    private <T> void saveIdempotencyKey(UUID key, UUID ownerId, String endpoint, int status,
                                        T responseBody, Instant now) {
        var body = jsonMapper.writeValueAsString(responseBody);
        var entry = IdempotencyKey.builder()
                .idempotencyKey(key)
                .ownerId(ownerId)
                .endpoint(endpoint)
                .responseStatus(status)
                .responseBody(body)
                .createdAt(now)
                .expiresAt(now.plus(IDEMPOTENCY_KEY_TTL_HOURS, ChronoUnit.HOURS))
                .build();
        idempotencyKeyRepository.save(entry);
    }

    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<T> toResponse(IdempotencyKey existing, Class<T> responseType) {
        try {
            var body = jsonMapper.readValue(existing.responseBody(), responseType);
            return ResponseEntity.status(existing.responseStatus()).body(body);
        } catch (Exception e) {
            log.warn("Failed to deserialize idempotency response key={}: {}", existing.idempotencyKey(), e.getMessage());
            return (ResponseEntity<T>) ResponseEntity.status(existing.responseStatus()).build();
        }
    }
}

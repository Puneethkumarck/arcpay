package com.arcpay.identity.agentidentity.infrastructure.db.idempotency.mapper;

import static com.arcpay.identity.agentidentity.fixtures.IdempotencyKeyFixtures.SOME_IDEMPOTENCY_KEY;
import static com.arcpay.identity.agentidentity.fixtures.IdempotencyKeyFixtures.SOME_IDEMPOTENCY_KEY_ENTITY;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyKeyEntityMapperTest {

    private final IdempotencyKeyEntityMapper mapper = Mappers.getMapper(IdempotencyKeyEntityMapper.class);

    @Test
    void shouldMapDomainToEntity() {
        // given
        var domain = SOME_IDEMPOTENCY_KEY;

        // when
        var result = mapper.mapToEntity(domain);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(SOME_IDEMPOTENCY_KEY_ENTITY);
    }

    @Test
    void shouldMapEntityToDomain() {
        // given
        var entity = SOME_IDEMPOTENCY_KEY_ENTITY;

        // when
        var result = mapper.mapToDomain(entity);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(SOME_IDEMPOTENCY_KEY);
    }
}

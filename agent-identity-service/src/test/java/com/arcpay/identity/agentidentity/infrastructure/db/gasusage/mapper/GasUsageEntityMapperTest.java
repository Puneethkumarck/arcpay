package com.arcpay.identity.agentidentity.infrastructure.db.gasusage.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static com.arcpay.identity.agentidentity.fixtures.GasUsageFixtures.SOME_GAS_USAGE;
import static com.arcpay.identity.agentidentity.fixtures.GasUsageFixtures.SOME_GAS_USAGE_WITHOUT_AGENT;
import static org.assertj.core.api.Assertions.assertThat;

class GasUsageEntityMapperTest {

    private final GasUsageEntityMapper mapper = Mappers.getMapper(GasUsageEntityMapper.class);

    @Test
    void shouldMapDomainToEntity() {
        // given
        var domain = SOME_GAS_USAGE;

        // when
        var entity = mapper.mapToEntity(domain);

        // then
        assertThat(entity)
                .usingRecursiveComparison()
                .isEqualTo(domain);
    }

    @Test
    void shouldMapEntityToDomain() {
        // given
        var entity = mapper.mapToEntity(SOME_GAS_USAGE);

        // when
        var domain = mapper.mapToDomain(entity);

        // then
        assertThat(domain)
                .usingRecursiveComparison()
                .isEqualTo(SOME_GAS_USAGE);
    }

    @Test
    void shouldPreserveBigDecimalPrecisionWhenMapping() {
        // given
        var preciseValue = new BigDecimal("0.00123456");
        var domain = SOME_GAS_USAGE.toBuilder().gasCostUsdc(preciseValue).build();

        // when
        var roundTrip = mapper.mapToDomain(mapper.mapToEntity(domain));

        // then
        assertThat(roundTrip)
                .usingRecursiveComparison()
                .isEqualTo(domain);
        assertThat(roundTrip.gasCostUsdc()).isEqualByComparingTo(preciseValue);
        assertThat(roundTrip.gasCostUsdc().scale()).isEqualTo(8);
    }

    @Test
    void shouldHandleNullableAgentIdWhenMappingToEntity() {
        // given
        var domain = SOME_GAS_USAGE_WITHOUT_AGENT;

        // when
        var entity = mapper.mapToEntity(domain);

        // then
        assertThat(entity)
                .usingRecursiveComparison()
                .isEqualTo(domain);
        assertThat(entity.getAgentId()).isNull();
    }

    @Test
    void shouldHandleNullableAgentIdWhenMappingToDomain() {
        // given
        var entity = mapper.mapToEntity(SOME_GAS_USAGE_WITHOUT_AGENT);

        // when
        var domain = mapper.mapToDomain(entity);

        // then
        assertThat(domain)
                .usingRecursiveComparison()
                .isEqualTo(SOME_GAS_USAGE_WITHOUT_AGENT);
        assertThat(domain.agentId()).isNull();
    }
}

package com.arcpay.identity.agentidentity.infrastructure.db.owner.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.someOwnerEntity;
import static org.assertj.core.api.Assertions.assertThat;

class OwnerEntityMapperTest {

    private final OwnerEntityMapper mapper = Mappers.getMapper(OwnerEntityMapper.class);

    @Test
    void shouldMapDomainOwnerToEntity() {
        // given
        var owner = SOME_OWNER;

        // when
        var result = mapper.mapToEntity(owner);

        // then
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(someOwnerEntity());
    }

    @Test
    void shouldMapEntityToDomainOwner() {
        // given
        var entity = someOwnerEntity();

        // when
        var result = mapper.mapToDomain(entity);

        // then
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(SOME_OWNER);
    }
}

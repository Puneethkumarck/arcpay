package com.arcpay.identity.agentidentity.infrastructure.db.owner.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER_ENTITY;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
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
                .isEqualTo(SOME_OWNER_ENTITY);
    }

    @Test
    void shouldMapEntityToDomainOwner() {
        // given
        var entity = SOME_OWNER_ENTITY;

        // when
        var result = mapper.mapToDomain(entity);

        // then
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(SOME_OWNER);
    }
}

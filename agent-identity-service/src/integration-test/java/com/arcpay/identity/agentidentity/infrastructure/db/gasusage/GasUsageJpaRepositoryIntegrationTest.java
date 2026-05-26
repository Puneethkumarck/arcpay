package com.arcpay.identity.agentidentity.infrastructure.db.gasusage;

import com.arcpay.identity.agentidentity.domain.port.GasUsageRepository;
import com.arcpay.identity.agentidentity.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.arcpay.identity.agentidentity.fixtures.GasUsageFixtures.SOME_GAS_USAGE;
import static com.arcpay.identity.agentidentity.fixtures.GasUsageFixtures.SOME_GAS_USAGE_WITHOUT_AGENT;
import static com.arcpay.identity.agentidentity.fixtures.GasUsageFixtures.SOME_OWNER_ID;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class GasUsageJpaRepositoryIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private GasUsageRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveGasUsageAndRoundTrip() {
        // given
        insertOwner(SOME_OWNER_ID);

        // when
        var saved = repository.save(SOME_GAS_USAGE);

        // then
        assertThat(saved)
                .usingRecursiveComparison()
                .isEqualTo(SOME_GAS_USAGE);
    }

    @Test
    void shouldQueryByOwnerIdWithPagination() {
        // given
        insertOwner(SOME_OWNER_ID);
        repository.save(SOME_GAS_USAGE);

        // when
        var page = repository.findByOwnerId(SOME_OWNER_ID, PageRequest.of(0, 10));

        // then
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent())
                .singleElement()
                .usingRecursiveComparison()
                .isEqualTo(SOME_GAS_USAGE);
    }

    @Test
    void shouldHandleNullableAgentId() {
        // given
        insertOwner(SOME_OWNER_ID);

        // when
        var saved = repository.save(SOME_GAS_USAGE_WITHOUT_AGENT);

        // then
        assertThat(saved)
                .usingRecursiveComparison()
                .isEqualTo(SOME_GAS_USAGE_WITHOUT_AGENT);
        assertThat(saved.agentId()).isNull();
    }

    @Test
    void shouldPreserveBigDecimalPrecision() {
        // given
        insertOwner(SOME_OWNER_ID);
        var preciseValue = new BigDecimal("0.00123456");
        var input = SOME_GAS_USAGE.toBuilder().gasCostUsdc(preciseValue).build();
        repository.save(input);

        // when
        var page = repository.findByOwnerId(SOME_OWNER_ID, PageRequest.of(0, 10));

        // then
        var loaded = page.getContent().getFirst();
        assertThat(loaded)
                .usingRecursiveComparison()
                .isEqualTo(input);
        assertThat(loaded.gasCostUsdc()).isEqualByComparingTo(preciseValue);
        assertThat(loaded.gasCostUsdc().scale()).isEqualTo(8);
    }

    private void insertOwner(UUID ownerId) {
        var ownerHex = ownerId.toString().replace("-", "");
        jdbcTemplate.update(
                "INSERT INTO owners (owner_id, email, wallet_address, api_key_hash, status) " +
                        "VALUES (?, ?, ?, ?, ?)",
                ownerId,
                "owner-" + ownerId + "@test.example",
                "0x" + (ownerHex + ownerHex).substring(0, 40),
                (ownerHex + ownerHex).substring(0, 64),
                "ACTIVE"
        );
    }
}

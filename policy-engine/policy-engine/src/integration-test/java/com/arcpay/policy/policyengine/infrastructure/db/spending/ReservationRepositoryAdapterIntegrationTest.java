package com.arcpay.policy.policyengine.infrastructure.db.spending;

import com.arcpay.policy.policyengine.domain.model.Reservation;
import com.arcpay.policy.policyengine.domain.model.ReservationStatus;
import com.arcpay.policy.policyengine.domain.port.ReservationRepository;
import com.arcpay.policy.policyengine.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ReservationRepositoryAdapterIntegrationTest extends FullContextIntegrationTest {

    private static final String RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void shouldSaveAndFindByPaymentId() {
        // given
        var reservation = Reservation.held(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.000000"), RECIPIENT, Instant.now().truncatedTo(ChronoUnit.MICROS));

        // when
        reservationRepository.save(reservation);
        var loaded = reservationRepository.findByPaymentId(reservation.paymentId()).orElseThrow();

        // then
        assertThat(loaded).usingRecursiveComparison().isEqualTo(reservation);
    }

    @Test
    void shouldSumOnlyActiveHeldReservations() {
        // given
        var agentId = UUID.randomUUID();
        var createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        reservationRepository.save(Reservation.held(UUID.randomUUID(), agentId, new BigDecimal("40.000000"), RECIPIENT, createdAt));
        reservationRepository.save(Reservation.held(UUID.randomUUID(), agentId, new BigDecimal("60.000000"), RECIPIENT, createdAt));
        reservationRepository.save(Reservation.held(UUID.randomUUID(), agentId, new BigDecimal("100.000000"), RECIPIENT, createdAt).commit());
        reservationRepository.save(Reservation.held(UUID.randomUUID(), agentId, new BigDecimal("30.000000"), RECIPIENT, createdAt).release());

        // when
        var sum = reservationRepository.sumActiveHeldAmount(agentId);

        // then
        assertThat(sum).isEqualByComparingTo("100.000000");
    }

    @Test
    void shouldReturnZeroWhenNoHeldReservations() {
        // given
        var agentId = UUID.randomUUID();

        // when
        var sum = reservationRepository.sumActiveHeldAmount(agentId);

        // then
        assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldUpdateStatusOnSaveByPrimaryKey() {
        // given
        var reservation = Reservation.held(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.000000"), RECIPIENT, Instant.now().truncatedTo(ChronoUnit.MICROS));
        reservationRepository.save(reservation);

        // when
        reservationRepository.save(reservation.commit());
        var loaded = reservationRepository.findByPaymentId(reservation.paymentId()).orElseThrow();

        // then
        assertThat(loaded.status()).isEqualTo(ReservationStatus.COMMITTED);
    }
}

package com.arcpay.payment.paymentexecution.application.service;

import com.arcpay.payment.paymentexecution.domain.exception.PaymentAccessDeniedException;
import com.arcpay.payment.paymentexecution.domain.exception.PaymentNotFoundException;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.port.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_AGENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_OWNER_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_PAYMENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.somePayment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PaymentQueryServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentQueryService paymentQueryService;

    @Test
    void shouldGetPaymentForOwner() {
        // given
        var payment = somePayment(PaymentStatus.PENDING);
        given(paymentRepository.findById(SOME_PAYMENT_ID)).willReturn(Optional.of(payment));

        // when
        var result = paymentQueryService.getPayment(SOME_PAYMENT_ID, SOME_OWNER_ID);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(payment);
    }

    @Test
    void shouldThrowNotFoundWhenMissing() {
        // given
        given(paymentRepository.findById(SOME_PAYMENT_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> paymentQueryService.getPayment(SOME_PAYMENT_ID, SOME_OWNER_ID))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void shouldThrowAccessDeniedWhenNotOwner() {
        // given
        var payment = somePayment(PaymentStatus.PENDING);
        given(paymentRepository.findById(SOME_PAYMENT_ID)).willReturn(Optional.of(payment));
        var otherOwner = UUID.randomUUID();

        // when / then
        assertThatThrownBy(() -> paymentQueryService.getPayment(SOME_PAYMENT_ID, otherOwner))
                .isInstanceOf(PaymentAccessDeniedException.class);
    }

    @Test
    void shouldListByOwnerWhenNoFilters() {
        // given
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(somePayment(PaymentStatus.PENDING)), pageable, 1);
        given(paymentRepository.findByOwnerId(SOME_OWNER_ID, pageable)).willReturn(page);

        // when
        var result = paymentQueryService.listPayments(SOME_OWNER_ID, null, null, pageable);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(page);
    }

    @Test
    void shouldListByOwnerAndAgentAndStatus() {
        // given
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(somePayment(PaymentStatus.COMPLETED)), pageable, 1);
        given(paymentRepository.findByOwnerIdAndAgentIdAndStatus(
                SOME_OWNER_ID, SOME_AGENT_ID, PaymentStatus.COMPLETED, pageable)).willReturn(page);

        // when
        var result = paymentQueryService.listPayments(
                SOME_OWNER_ID, SOME_AGENT_ID, PaymentStatus.COMPLETED, pageable);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(page);
    }
}

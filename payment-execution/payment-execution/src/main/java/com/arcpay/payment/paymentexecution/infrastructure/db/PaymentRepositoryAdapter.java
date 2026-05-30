package com.arcpay.payment.paymentexecution.infrastructure.db;

import com.arcpay.payment.paymentexecution.domain.exception.IdempotencyConflictException;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;
    private final PaymentEntityMapper mapper;

    @Override
    public Payment save(Payment payment) {
        var existing = jpaRepository.findByAgentIdAndIdempotencyKey(payment.agentId(), payment.idempotencyKey());
        if (existing.isPresent()) {
            return resolveIdempotentReplay(payment, existing.get());
        }
        return mapper.mapToDomain(jpaRepository.saveAndFlush(mapper.mapToEntity(payment)));
    }

    private Payment resolveIdempotentReplay(Payment payment, PaymentEntity existing) {
        if (!existing.getRequestFingerprint().equals(payment.requestFingerprint())) {
            throw new IdempotencyConflictException(
                    "Idempotency key '%s' for agent %s was reused with a different request fingerprint"
                            .formatted(payment.idempotencyKey(), payment.agentId()));
        }
        return mapper.mapToDomain(existing);
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return jpaRepository.findById(paymentId).map(mapper::mapToDomain);
    }

    @Override
    public Optional<Payment> findByAgentIdAndIdempotencyKey(UUID agentId, String idempotencyKey) {
        return jpaRepository.findByAgentIdAndIdempotencyKey(agentId, idempotencyKey).map(mapper::mapToDomain);
    }

    @Override
    public Page<Payment> findByOwnerId(UUID ownerId, Pageable pageable) {
        return jpaRepository.findByOwnerId(ownerId, pageable).map(mapper::mapToDomain);
    }

    @Override
    public Page<Payment> findByOwnerIdAndAgentId(UUID ownerId, UUID agentId, Pageable pageable) {
        return jpaRepository.findByOwnerIdAndAgentId(ownerId, agentId, pageable).map(mapper::mapToDomain);
    }

    @Override
    public Page<Payment> findByOwnerIdAndAgentIdAndStatus(UUID ownerId, UUID agentId, PaymentStatus status, Pageable pageable) {
        return jpaRepository.findByOwnerIdAndAgentIdAndStatus(ownerId, agentId, status, pageable).map(mapper::mapToDomain);
    }

    @Override
    public Page<Payment> findByOwnerIdAndStatus(UUID ownerId, PaymentStatus status, Pageable pageable) {
        return jpaRepository.findByOwnerIdAndStatus(ownerId, status, pageable).map(mapper::mapToDomain);
    }
}

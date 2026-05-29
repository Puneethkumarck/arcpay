package com.arcpay.compliance.infrastructure.db;

import com.arcpay.compliance.domain.exception.ScreeningAlreadyExistsException;
import com.arcpay.compliance.domain.model.ScreeningCheck;
import com.arcpay.compliance.domain.model.ScreeningResult;
import com.arcpay.compliance.domain.port.ScreeningStore;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ScreeningStoreAdapter implements ScreeningStore {

    private final ScreeningResultRepository screeningResultRepository;
    private final ScreeningCheckRepository screeningCheckRepository;
    private final ScreeningResultMapper screeningResultMapper;
    private final ScreeningCheckMapper screeningCheckMapper;

    @Override
    @Transactional
    public void insert(ScreeningResult result, List<ScreeningCheck> checks) {
        try {
            screeningResultRepository.saveAndFlush(screeningResultMapper.mapToEntity(result));
        } catch (DataIntegrityViolationException e) {
            throw new ScreeningAlreadyExistsException(result.paymentId(), e);
        }
        for (var check : checks) {
            screeningCheckRepository.save(
                    screeningCheckMapper.mapToEntity(check, UuidCreator.getTimeOrderedEpoch(), result.screeningId()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ScreeningResult> findByPaymentId(UUID paymentId) {
        return screeningResultRepository.findByPaymentId(paymentId)
                .map(this::toDomainWithChecks);
    }

    private ScreeningResult toDomainWithChecks(ScreeningResultEntity entity) {
        var checks = screeningCheckRepository.findByScreeningId(entity.getScreeningId()).stream()
                .map(screeningCheckMapper::mapToDomain)
                .toList();
        return screeningResultMapper.mapToDomain(entity).toBuilder()
                .checks(checks)
                .build();
    }
}

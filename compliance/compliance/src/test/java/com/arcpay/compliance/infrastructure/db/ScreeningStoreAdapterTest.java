package com.arcpay.compliance.infrastructure.db;

import com.arcpay.compliance.domain.exception.ScreeningAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_CLEAR_CHECK;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_PAYMENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SCREENING_RESULT_PASS;
import static com.arcpay.platform.test.TestUtils.eqIgnoring;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ScreeningStoreAdapterTest {

    @Mock
    private ScreeningResultRepository screeningResultRepository;

    @Mock
    private ScreeningCheckRepository screeningCheckRepository;

    private final ScreeningResultMapper screeningResultMapper = Mappers.getMapper(ScreeningResultMapper.class);
    private final ScreeningCheckMapper screeningCheckMapper = Mappers.getMapper(ScreeningCheckMapper.class);

    private ScreeningStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ScreeningStoreAdapter(
                screeningResultRepository, screeningCheckRepository, screeningResultMapper, screeningCheckMapper);
    }

    @Test
    void shouldInsertScreeningResultAndMapToEntity() {
        // given
        var expectedEntity = screeningResultMapper.mapToEntity(SOME_SCREENING_RESULT_PASS);

        // when
        adapter.insert(SOME_SCREENING_RESULT_PASS, List.of(SOME_CLEAR_CHECK));

        // then
        then(screeningResultRepository).should().saveAndFlush(eqIgnoring(expectedEntity, "checks"));
    }

    @Test
    void shouldInsertScreeningChecks() {
        // given
        var expectedCheckEntity = screeningCheckMapper.mapToEntity(
                SOME_CLEAR_CHECK, null, SOME_SCREENING_RESULT_PASS.screeningId());

        // when
        adapter.insert(SOME_SCREENING_RESULT_PASS, List.of(SOME_CLEAR_CHECK));

        // then
        then(screeningCheckRepository).should().save(eqIgnoring(expectedCheckEntity, "id"));
    }

    @Test
    void shouldThrowScreeningAlreadyExistsOnDuplicatePaymentId() {
        // given
        var expectedEntity = screeningResultMapper.mapToEntity(SOME_SCREENING_RESULT_PASS);
        given(screeningResultRepository.saveAndFlush(eqIgnoring(expectedEntity, "checks")))
                .willThrow(new DataIntegrityViolationException("duplicate payment_id"));

        // when / then
        assertThatThrownBy(() -> adapter.insert(SOME_SCREENING_RESULT_PASS, List.of(SOME_CLEAR_CHECK)))
                .isInstanceOf(ScreeningAlreadyExistsException.class)
                .hasMessageContaining(SOME_PAYMENT_ID.toString());
        then(screeningCheckRepository).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnEmptyWhenScreeningNotFoundByPaymentId() {
        // given
        given(screeningResultRepository.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.empty());

        // when
        var result = adapter.findByPaymentId(SOME_PAYMENT_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindScreeningByPaymentIdWithChecks() {
        // given
        var resultEntity = screeningResultMapper.mapToEntity(SOME_SCREENING_RESULT_PASS);
        var checkEntity = screeningCheckMapper.mapToEntity(
                SOME_CLEAR_CHECK, null, SOME_SCREENING_RESULT_PASS.screeningId());
        given(screeningResultRepository.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.of(resultEntity));
        given(screeningCheckRepository.findByScreeningId(SOME_SCREENING_RESULT_PASS.screeningId()))
                .willReturn(List.of(checkEntity));

        // when
        var result = adapter.findByPaymentId(SOME_PAYMENT_ID);

        // then
        assertThat(result).get().usingRecursiveComparison().isEqualTo(SOME_SCREENING_RESULT_PASS);
    }
}

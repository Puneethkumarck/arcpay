package com.arcpay.compliance.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_CLEAR_CHECK;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SCREENING_RESULT_PASS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScreeningResultTest {

    @Test
    void shouldBuildScreeningResult() {
        // given
        var result = SOME_SCREENING_RESULT_PASS;

        // when
        var rebuilt = result.toBuilder().build();

        // then
        var expected = SOME_SCREENING_RESULT_PASS.toBuilder()
                .verdict(Verdict.PASS)
                .checks(List.of(SOME_CLEAR_CHECK))
                .build();
        assertThat(rebuilt).usingRecursiveComparison().isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("nullRequiredFields")
    void shouldRejectNullRequiredField(String fieldName, UnaryOperator<ScreeningResult.ScreeningResultBuilder> mutator) {
        // given
        var builder = mutator.apply(SOME_SCREENING_RESULT_PASS.toBuilder());

        // when / then
        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(fieldName);
    }

    static Stream<Arguments> nullRequiredFields() {
        return Stream.of(
                Arguments.of("paymentId", (UnaryOperator<ScreeningResult.ScreeningResultBuilder>) b -> b.paymentId(null)),
                Arguments.of("agentId", (UnaryOperator<ScreeningResult.ScreeningResultBuilder>) b -> b.agentId(null)),
                Arguments.of("recipientAddress", (UnaryOperator<ScreeningResult.ScreeningResultBuilder>) b -> b.recipientAddress(null)),
                Arguments.of("verdict", (UnaryOperator<ScreeningResult.ScreeningResultBuilder>) b -> b.verdict(null))
        );
    }
}

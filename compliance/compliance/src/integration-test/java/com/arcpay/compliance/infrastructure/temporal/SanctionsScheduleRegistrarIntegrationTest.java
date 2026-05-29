package com.arcpay.compliance.infrastructure.temporal;

import com.arcpay.compliance.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThatCode;

class SanctionsScheduleRegistrarIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private SanctionsScheduleRegistrar registrar;

    @Test
    void shouldRegisterScheduleOnStartupWithoutError() {
        // when / then
        assertThatCode(registrar::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void shouldBeIdempotentWhenScheduleAlreadyExists() {
        // given
        registrar.afterPropertiesSet();

        // when / then
        assertThatCode(registrar::afterPropertiesSet).doesNotThrowAnyException();
    }
}

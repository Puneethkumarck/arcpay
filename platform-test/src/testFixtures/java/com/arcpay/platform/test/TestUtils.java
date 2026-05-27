package com.arcpay.platform.test;

import org.mockito.ArgumentMatcher;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;

public final class TestUtils {

    private static final Set<Class<?>> TIMESTAMP_TYPES = Set.of(
            Instant.class,
            LocalDateTime.class,
            LocalDate.class,
            ZonedDateTime.class
    );

    private TestUtils() {}

    public static <T> T eqIgnoringTimestamps(T expected) {
        return argThat(new RecursiveComparisonIgnoringTimestamps<>(expected));
    }

    public static <T> T eqIgnoring(T expected, String... fieldsToIgnore) {
        return argThat(new RecursiveComparisonIgnoringFields<>(expected, fieldsToIgnore));
    }

    private record RecursiveComparisonIgnoringTimestamps<T>(T expected) implements ArgumentMatcher<T> {

        @Override
        public boolean matches(T actual) {
            if (actual == null) {
                return false;
            }
            try {
                assertThat(actual)
                        .usingRecursiveComparison()
                        .ignoringFieldsOfTypes(TIMESTAMP_TYPES.toArray(Class<?>[]::new))
                        .isEqualTo(expected);
                return true;
            } catch (AssertionError e) {
                return false;
            }
        }

        @Override
        public String toString() {
            return "eqIgnoringTimestamps(" + expected + ")";
        }
    }

    private record RecursiveComparisonIgnoringFields<T>(T expected, String[] fieldsToIgnore) implements ArgumentMatcher<T> {

        @Override
        public boolean matches(T actual) {
            if (actual == null) {
                return false;
            }
            try {
                assertThat(actual)
                        .usingRecursiveComparison()
                        .ignoringFields(fieldsToIgnore)
                        .isEqualTo(expected);
                return true;
            } catch (AssertionError e) {
                return false;
            }
        }

        @Override
        public String toString() {
            return "eqIgnoring(" + expected + ", " + Arrays.toString(fieldsToIgnore) + ")";
        }
    }
}

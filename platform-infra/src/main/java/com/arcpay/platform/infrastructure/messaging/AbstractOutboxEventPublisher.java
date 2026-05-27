package com.arcpay.platform.infrastructure.messaging;

import io.namastack.outbox.Outbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractOutboxEventPublisher {

    private final Outbox outbox;
    private final List<String> keyFieldNames;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(Object event) {
        Objects.requireNonNull(event, "event must not be null");
        var key = resolveKey(event);
        outbox.schedule(event, key);
        log.debug("Scheduled outbox event type={} key={}", event.getClass().getSimpleName(), key);
    }

    private String resolveKey(Object event) {
        for (String fieldName : keyFieldNames) {
            try {
                var method = event.getClass().getMethod(fieldName);
                method.setAccessible(true);
                var value = method.invoke(event);
                if (value != null) {
                    return String.valueOf(value);
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Error invoking accessor '" + fieldName + "' on " + event.getClass().getName(), e);
            }
        }
        throw new IllegalArgumentException(
                "Event class has no non-null value for any of " + keyFieldNames + ": " + event.getClass().getName());
    }
}

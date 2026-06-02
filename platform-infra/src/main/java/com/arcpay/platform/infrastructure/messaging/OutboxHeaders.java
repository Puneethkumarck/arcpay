package com.arcpay.platform.infrastructure.messaging;

public final class OutboxHeaders {

    public static final String EVENT_ID_HEADER = "X-Event-Id";

    public static final String EVENT_ID_CONTEXT_KEY = "eventId";

    private OutboxHeaders() {}
}

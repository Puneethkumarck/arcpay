package com.arcpay.settlement.domain.model;

public enum TransferState {
    INITIATED,
    QUEUED,
    SENT,
    CONFIRMED,
    COMPLETED,
    FAILED,
    DENIED,
    CANCELLED,
    STUCK
}

package com.arcpay.compliance.infrastructure.temporal;

public class SanctionsDownloadException extends RuntimeException {

    public SanctionsDownloadException(String message) {
        super(message);
    }

    public SanctionsDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}

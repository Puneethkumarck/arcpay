package com.arcpay.compliance.infrastructure.temporal;

import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;

public interface SanctionsFeedDownloader {

    byte[] download(SanctionsSource source);
}

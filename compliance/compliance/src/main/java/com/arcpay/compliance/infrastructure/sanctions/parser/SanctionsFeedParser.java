package com.arcpay.compliance.infrastructure.sanctions.parser;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;

import java.util.List;

public interface SanctionsFeedParser {

    SanctionsSource source();

    List<SanctionedAddressRecord> parse(String rawFeedContent);
}

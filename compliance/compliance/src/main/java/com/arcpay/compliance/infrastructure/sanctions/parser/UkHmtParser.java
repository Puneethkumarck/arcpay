package com.arcpay.compliance.infrastructure.sanctions.parser;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.UK_HMT;

@Component
public class UkHmtParser implements SanctionsFeedParser {

    @Override
    public SanctionsSource source() {
        return UK_HMT;
    }

    @Override
    public List<SanctionedAddressRecord> parse(String rawFeedContent) {
        return EvmAddressExtractor.extract(rawFeedContent, UK_HMT);
    }
}

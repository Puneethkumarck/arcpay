package com.arcpay.compliance.infrastructure.sanctions.parser;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.EU;

@Component
public class EuParser implements SanctionsFeedParser {

    @Override
    public SanctionsSource source() {
        return EU;
    }

    @Override
    public List<SanctionedAddressRecord> parse(String rawFeedContent) {
        return EvmAddressExtractor.extract(rawFeedContent, EU);
    }
}

package com.arcpay.compliance.infrastructure.sanctions.parser;

import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.EU;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import java.util.List;
import org.springframework.stereotype.Component;

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

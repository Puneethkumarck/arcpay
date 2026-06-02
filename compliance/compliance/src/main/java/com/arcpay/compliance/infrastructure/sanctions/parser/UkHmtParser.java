package com.arcpay.compliance.infrastructure.sanctions.parser;

import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.UK_HMT;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import java.util.List;
import org.springframework.stereotype.Component;

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

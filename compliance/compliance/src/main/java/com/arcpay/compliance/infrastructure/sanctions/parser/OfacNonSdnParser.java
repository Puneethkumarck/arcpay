package com.arcpay.compliance.infrastructure.sanctions.parser;

import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.OFAC_NONSDN;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OfacNonSdnParser implements SanctionsFeedParser {

    @Override
    public SanctionsSource source() {
        return OFAC_NONSDN;
    }

    @Override
    public List<SanctionedAddressRecord> parse(String rawFeedContent) {
        return EvmAddressExtractor.extract(rawFeedContent, OFAC_NONSDN);
    }
}

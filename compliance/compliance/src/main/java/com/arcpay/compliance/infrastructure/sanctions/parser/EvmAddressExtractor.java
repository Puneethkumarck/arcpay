package com.arcpay.compliance.infrastructure.sanctions.parser;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

final class EvmAddressExtractor {

    private static final Pattern EVM_ADDRESS = Pattern.compile("(?<![0-9a-fA-Fx])0x[0-9a-fA-F]{40}(?![0-9a-fA-F])");

    private EvmAddressExtractor() {}

    static List<SanctionedAddressRecord> extract(String rawFeedContent, SanctionsSource source) {
        if (rawFeedContent == null || rawFeedContent.isBlank()) {
            return List.of();
        }
        var seen = new LinkedHashSet<String>();
        var records = new ArrayList<SanctionedAddressRecord>();
        var matcher = EVM_ADDRESS.matcher(rawFeedContent);
        while (matcher.find()) {
            var normalized = matcher.group().toLowerCase();
            if (seen.add(normalized)) {
                records.add(SanctionedAddressRecord.builder()
                        .address(normalized)
                        .source(source)
                        .sourceRef(null)
                        .build());
            }
        }
        return List.copyOf(records);
    }
}

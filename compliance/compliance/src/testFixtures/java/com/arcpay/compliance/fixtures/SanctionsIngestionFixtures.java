package com.arcpay.compliance.fixtures;

import com.arcpay.compliance.infrastructure.sanctions.SanctionedAddressRecord;
import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.EU;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.OFAC_NONSDN;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.OFAC_SDN;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.UK_HMT;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.UN;

public final class SanctionsIngestionFixtures {

    public static final String SOME_TRIGGER_TIMESTAMP = "2026-06-01T00:00:00Z";

    public static final String SOME_OFAC_SDN_ADDRESS = "0xa1b2c3d4e5f60718293a4b5c6d7e8f9001122334";
    public static final String SOME_OFAC_NONSDN_ADDRESS = "0x1122334455667788990011223344556677889900";
    public static final String SOME_UN_ADDRESS = "0x2222222222222222222222222222222222222222";
    public static final String SOME_EU_ADDRESS = "0x3333333333333333333333333333333333333333";
    public static final String SOME_UK_HMT_ADDRESS = "0x4444444444444444444444444444444444444444";

    public static final SanctionedAddressRecord SOME_OFAC_SDN_RECORD = record(SOME_OFAC_SDN_ADDRESS, OFAC_SDN);
    public static final SanctionedAddressRecord SOME_OFAC_NONSDN_RECORD = record(SOME_OFAC_NONSDN_ADDRESS, OFAC_NONSDN);
    public static final SanctionedAddressRecord SOME_UN_RECORD = record(SOME_UN_ADDRESS, UN);
    public static final SanctionedAddressRecord SOME_EU_RECORD = record(SOME_EU_ADDRESS, EU);
    public static final SanctionedAddressRecord SOME_UK_HMT_RECORD = record(SOME_UK_HMT_ADDRESS, UK_HMT);

    public static final Map<SanctionsSource, List<SanctionedAddressRecord>> SOME_ALL_SOURCES_RECORDS = Map.of(
            OFAC_SDN, List.of(SOME_OFAC_SDN_RECORD),
            OFAC_NONSDN, List.of(SOME_OFAC_NONSDN_RECORD),
            UN, List.of(SOME_UN_RECORD),
            EU, List.of(SOME_EU_RECORD),
            UK_HMT, List.of(SOME_UK_HMT_RECORD));

    public static byte[] feedBytes(String feed) {
        return feed.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] feedFor(SanctionsSource source) {
        return feedBytes("Digital Currency Address - ETH " + addressFor(source) + "\n");
    }

    public static String addressFor(SanctionsSource source) {
        return switch (source) {
            case OFAC_SDN -> SOME_OFAC_SDN_ADDRESS;
            case OFAC_NONSDN -> SOME_OFAC_NONSDN_ADDRESS;
            case UN -> SOME_UN_ADDRESS;
            case EU -> SOME_EU_ADDRESS;
            case UK_HMT -> SOME_UK_HMT_ADDRESS;
        };
    }

    private static SanctionedAddressRecord record(String address, SanctionsSource source) {
        return SanctionedAddressRecord.builder()
                .address(address)
                .source(source)
                .sourceRef(null)
                .build();
    }

    private SanctionsIngestionFixtures() {}
}

package com.arcpay.compliance.fixtures;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class SanctionsFeedFixtures {

    public static final String OFAC_SDN_FEED = load("feeds/ofac-sdn-sample.xml");
    public static final String OFAC_NONSDN_FEED = load("feeds/ofac-nonsdn-sample.csv");
    public static final String UN_FEED = load("feeds/un-sample.xml");
    public static final String EU_FEED = load("feeds/eu-sample.xml");
    public static final String UK_HMT_FEED = load("feeds/uk-hmt-sample.csv");

    public static final Set<String> EXPECTED_OFAC_SDN_ADDRESSES = Set.of(
            "0xa1b2c3d4e5f60718293a4b5c6d7e8f9001122334",
            "0xffffffffffffffffffffffffffffffffffffffff");

    public static final Set<String> EXPECTED_OFAC_NONSDN_ADDRESSES = Set.of(
            "0x1122334455667788990011223344556677889900");

    public static final String SOME_OTHER_CHAIN_BTC_ADDRESS = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa";

    public static String uniqueEvmAddress() {
        var hex = Long.toHexString(System.nanoTime());
        return ("0x" + "0".repeat(40 - hex.length()) + hex).toLowerCase();
    }

    private static String load(String resource) {
        try (var stream = SanctionsFeedFixtures.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Missing sanctions feed fixture: " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private SanctionsFeedFixtures() {}
}

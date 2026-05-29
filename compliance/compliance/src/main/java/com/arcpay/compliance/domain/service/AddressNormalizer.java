package com.arcpay.compliance.domain.service;

import com.arcpay.compliance.domain.exception.MalformedAddressException;

import java.util.Locale;
import java.util.regex.Pattern;

final class AddressNormalizer {

    private static final Pattern EVM_ADDRESS = Pattern.compile("^0x[0-9a-f]{40}$");

    private AddressNormalizer() {}

    static String normalize(String address) {
        if (address == null) {
            throw new MalformedAddressException("null");
        }
        var normalized = address.trim().toLowerCase(Locale.ROOT);
        if (!EVM_ADDRESS.matcher(normalized).matches()) {
            throw new MalformedAddressException(address);
        }
        return normalized;
    }
}

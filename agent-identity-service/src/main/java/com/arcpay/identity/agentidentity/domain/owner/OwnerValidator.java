package com.arcpay.identity.agentidentity.domain.owner;

import com.arcpay.identity.agentidentity.domain.exception.InvalidEmailException;
import com.arcpay.identity.agentidentity.domain.exception.InvalidWalletAddressException;
import com.arcpay.identity.agentidentity.domain.exception.OwnerEmailAlreadyExistsException;
import com.arcpay.identity.agentidentity.domain.exception.OwnerWalletAlreadyExistsException;
import com.arcpay.identity.agentidentity.domain.port.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class OwnerValidator {

    private static final int MAX_EMAIL_LENGTH = 255;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern WALLET_ADDRESS_PATTERN = Pattern.compile(
            "^0x[0-9a-fA-F]{40}$");

    private final OwnerRepository ownerRepository;

    public void validateRegistration(String email, String walletAddress) {
        validateEmail(email);
        validateWalletAddress(walletAddress);
        validateEmailUniqueness(email);
        validateWalletAddressUniqueness(walletAddress);
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidEmailException(email);
        }
        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new InvalidEmailException(email);
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidEmailException(email);
        }
    }

    private void validateWalletAddress(String walletAddress) {
        if (walletAddress == null || walletAddress.isBlank()) {
            throw new InvalidWalletAddressException(walletAddress);
        }
        if (!WALLET_ADDRESS_PATTERN.matcher(walletAddress).matches()) {
            throw new InvalidWalletAddressException(walletAddress);
        }
    }

    private void validateEmailUniqueness(String email) {
        if (ownerRepository.existsByEmailIgnoreCase(email)) {
            throw new OwnerEmailAlreadyExistsException(email);
        }
    }

    private void validateWalletAddressUniqueness(String walletAddress) {
        if (ownerRepository.existsByWalletAddressIgnoreCase(walletAddress)) {
            throw new OwnerWalletAlreadyExistsException(walletAddress);
        }
    }
}

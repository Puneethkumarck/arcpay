package com.arcpay.identity.agentidentity.domain.owner;

import com.arcpay.identity.agentidentity.domain.exception.InvalidEmailException;
import com.arcpay.identity.agentidentity.domain.exception.InvalidWalletAddressException;
import com.arcpay.identity.agentidentity.domain.exception.OwnerEmailAlreadyExistsException;
import com.arcpay.identity.agentidentity.domain.exception.OwnerWalletAlreadyExistsException;
import com.arcpay.identity.agentidentity.domain.port.OwnerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OwnerValidatorTest {

    private static final String VALID_EMAIL = "alice@example.com";
    private static final String VALID_WALLET = "0x1234567890abcdef1234567890abcdef12345678";

    @Mock
    private OwnerRepository ownerRepository;

    @InjectMocks
    private OwnerValidator ownerValidator;

    @Test
    void shouldAcceptValidEmail() {
        // given
        given(ownerRepository.existsByEmailIgnoreCase(VALID_EMAIL)).willReturn(false);
        given(ownerRepository.existsByWalletAddressIgnoreCase(VALID_WALLET)).willReturn(false);

        // when / then
        assertThatCode(() -> ownerValidator.validateRegistration(VALID_EMAIL, VALID_WALLET))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInvalidEmailFormat() {
        // given / when / then
        assertThatThrownBy(() -> ownerValidator.validateRegistration("not-an-email", VALID_WALLET))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessageContaining("Invalid email");
    }

    @Test
    void shouldRejectTooLongEmail() {
        // given
        var longEmail = "a".repeat(250) + "@b.com";

        // when / then
        assertThatThrownBy(() -> ownerValidator.validateRegistration(longEmail, VALID_WALLET))
                .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void shouldRejectEmptyEmail() {
        // given / when / then
        assertThatThrownBy(() -> ownerValidator.validateRegistration("", VALID_WALLET))
                .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void shouldRejectNullEmail() {
        // given / when / then
        assertThatThrownBy(() -> ownerValidator.validateRegistration(null, VALID_WALLET))
                .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void shouldAcceptValidWalletAddress() {
        // given
        given(ownerRepository.existsByEmailIgnoreCase(VALID_EMAIL)).willReturn(false);
        given(ownerRepository.existsByWalletAddressIgnoreCase(VALID_WALLET)).willReturn(false);

        // when / then
        assertThatCode(() -> ownerValidator.validateRegistration(VALID_EMAIL, VALID_WALLET))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptChecksumWalletAddress() {
        // given
        var checksumWallet = "0x1234567890AbCdEf1234567890aBcDeF12345678";
        given(ownerRepository.existsByEmailIgnoreCase(VALID_EMAIL)).willReturn(false);
        given(ownerRepository.existsByWalletAddressIgnoreCase(checksumWallet)).willReturn(false);

        // when / then
        assertThatCode(() -> ownerValidator.validateRegistration(VALID_EMAIL, checksumWallet))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectWalletAddressWrongLength() {
        // given / when / then
        assertThatThrownBy(() -> ownerValidator.validateRegistration(VALID_EMAIL, "0x1234"))
                .isInstanceOf(InvalidWalletAddressException.class)
                .hasMessageContaining("Invalid wallet address");
    }

    @Test
    void shouldRejectWalletAddressMissing0x() {
        // given
        var noPrefix = "1234567890abcdef1234567890abcdef12345678";

        // when / then
        assertThatThrownBy(() -> ownerValidator.validateRegistration(VALID_EMAIL, noPrefix))
                .isInstanceOf(InvalidWalletAddressException.class);
    }

    @Test
    void shouldRejectNonHexWalletAddress() {
        // given
        var nonHex = "0xGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG";

        // when / then
        assertThatThrownBy(() -> ownerValidator.validateRegistration(VALID_EMAIL, nonHex))
                .isInstanceOf(InvalidWalletAddressException.class);
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        // given
        given(ownerRepository.existsByEmailIgnoreCase(VALID_EMAIL)).willReturn(true);

        // when / then
        assertThatThrownBy(() -> ownerValidator.validateRegistration(VALID_EMAIL, VALID_WALLET))
                .isInstanceOf(OwnerEmailAlreadyExistsException.class)
                .hasMessageContaining(VALID_EMAIL);
    }

    @Test
    void shouldThrowWhenWalletAddressAlreadyExists() {
        // given
        given(ownerRepository.existsByEmailIgnoreCase(VALID_EMAIL)).willReturn(false);
        given(ownerRepository.existsByWalletAddressIgnoreCase(VALID_WALLET)).willReturn(true);

        // when / then
        assertThatThrownBy(() -> ownerValidator.validateRegistration(VALID_EMAIL, VALID_WALLET))
                .isInstanceOf(OwnerWalletAlreadyExistsException.class)
                .hasMessageContaining(VALID_WALLET);
    }
}

package com.arcpay.identity.agentidentity.domain.owner;

import com.arcpay.identity.agentidentity.domain.event.OwnerRegistered;
import com.arcpay.identity.agentidentity.domain.exception.InvalidEmailException;
import com.arcpay.identity.agentidentity.domain.exception.InvalidWalletAddressException;
import com.arcpay.identity.agentidentity.domain.exception.OwnerEmailAlreadyExistsException;
import com.arcpay.identity.agentidentity.domain.exception.OwnerWalletAlreadyExistsException;
import com.arcpay.identity.agentidentity.domain.model.Owner;
import com.arcpay.identity.agentidentity.domain.model.OwnerStatus;
import com.arcpay.identity.agentidentity.domain.model.OwnerWithApiKey;
import com.arcpay.identity.agentidentity.domain.port.EventPublisher;
import com.arcpay.identity.agentidentity.domain.port.OwnerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static com.arcpay.platform.test.TestUtils.eqIgnoring;
import static com.arcpay.platform.test.TestUtils.eqIgnoringTimestamps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class OwnerCommandHandlerTest {

    private static final String VALID_EMAIL = "alice@example.com";
    private static final String VALID_WALLET = "0x1234567890abcdef1234567890abcdef12345678";

    @Mock
    private OwnerValidator ownerValidator;

    @Mock
    private OwnerCreationService ownerCreationService;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private OwnerCommandHandler ownerCommandHandler;

    @Test
    void shouldRegisterOwnerAndPublishEvent() {
        // given
        var ownerId = UUID.fromString("019718a0-1234-7def-8000-abcdef123456");
        var now = Instant.now();
        var owner = Owner.builder()
                .ownerId(ownerId)
                .email(VALID_EMAIL)
                .walletAddress(VALID_WALLET)
                .apiKeyHash("somehash")
                .status(OwnerStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        var rawApiKey = "ak_test_abc123";
        var creationResult = new OwnerWithApiKey(owner, rawApiKey);

        given(ownerCreationService.createOwner(VALID_EMAIL, VALID_WALLET)).willReturn(creationResult);
        given(ownerRepository.save(eqIgnoringTimestamps(owner))).willReturn(owner);

        // when
        var result = ownerCommandHandler.registerOwner(VALID_EMAIL, VALID_WALLET);

        // then
        assertThat(result.owner()).usingRecursiveComparison().isEqualTo(owner);
        assertThat(result.rawApiKey()).isEqualTo(rawApiKey);

        then(ownerValidator).should().validateRegistration(VALID_EMAIL, VALID_WALLET);
        then(eventPublisher).should().publish(eqIgnoring(
                new OwnerRegistered(ownerId, VALID_EMAIL, VALID_WALLET, now),
                "registeredAt"));
    }

    @Test
    void shouldReturnRawApiKeyInResult() {
        // given
        var ownerId = UUID.randomUUID();
        var now = Instant.now();
        var owner = Owner.builder()
                .ownerId(ownerId)
                .email(VALID_EMAIL)
                .walletAddress(VALID_WALLET)
                .apiKeyHash("hash")
                .status(OwnerStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        var rawApiKey = "ak_test_secretkey123";
        given(ownerCreationService.createOwner(VALID_EMAIL, VALID_WALLET))
                .willReturn(new OwnerWithApiKey(owner, rawApiKey));
        given(ownerRepository.save(eqIgnoringTimestamps(owner))).willReturn(owner);

        // when
        var result = ownerCommandHandler.registerOwner(VALID_EMAIL, VALID_WALLET);

        // then
        var expected = new OwnerWithApiKey(owner, rawApiKey);
        assertThat(result).usingRecursiveComparison()
                .ignoringFields("owner.createdAt", "owner.updatedAt")
                .isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        // given
        willThrow(new OwnerEmailAlreadyExistsException(VALID_EMAIL))
                .given(ownerValidator).validateRegistration(VALID_EMAIL, VALID_WALLET);

        // when / then
        assertThatThrownBy(() -> ownerCommandHandler.registerOwner(VALID_EMAIL, VALID_WALLET))
                .isInstanceOf(OwnerEmailAlreadyExistsException.class);
    }

    @Test
    void shouldThrowWhenWalletAlreadyExists() {
        // given
        willThrow(new OwnerWalletAlreadyExistsException(VALID_WALLET))
                .given(ownerValidator).validateRegistration(VALID_EMAIL, VALID_WALLET);

        // when / then
        assertThatThrownBy(() -> ownerCommandHandler.registerOwner(VALID_EMAIL, VALID_WALLET))
                .isInstanceOf(OwnerWalletAlreadyExistsException.class);
    }

    @Test
    void shouldThrowWhenEmailInvalid() {
        // given
        willThrow(new InvalidEmailException("not-an-email"))
                .given(ownerValidator).validateRegistration("not-an-email", VALID_WALLET);

        // when / then
        assertThatThrownBy(() -> ownerCommandHandler.registerOwner("not-an-email", VALID_WALLET))
                .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void shouldThrowWhenWalletAddressInvalid() {
        // given
        willThrow(new InvalidWalletAddressException("0xshort"))
                .given(ownerValidator).validateRegistration(VALID_EMAIL, "0xshort");

        // when / then
        assertThatThrownBy(() -> ownerCommandHandler.registerOwner(VALID_EMAIL, "0xshort"))
                .isInstanceOf(InvalidWalletAddressException.class);
    }
}

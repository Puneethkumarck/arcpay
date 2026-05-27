package com.arcpay.identity.agentidentity.application.controller.internal;

import com.arcpay.identity.agentidentity.api.model.OwnerPrincipalResponse;
import com.arcpay.identity.agentidentity.domain.exception.OwnerNotFoundException;
import com.arcpay.identity.agentidentity.domain.port.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/owners")
@RequiredArgsConstructor
public class InternalOwnerController {

    private final OwnerRepository ownerRepository;

    @GetMapping("/by-api-key-hash/{hash}")
    public OwnerPrincipalResponse resolveByApiKeyHash(@PathVariable String hash) {
        log.info("Internal owner lookup by api-key-hash={}", hash);
        var owner = ownerRepository.findByApiKeyHash(hash)
                .orElseThrow(OwnerNotFoundException::new);
        return OwnerPrincipalResponse.builder()
                .ownerId(owner.ownerId())
                .email(owner.email())
                .build();
    }
}

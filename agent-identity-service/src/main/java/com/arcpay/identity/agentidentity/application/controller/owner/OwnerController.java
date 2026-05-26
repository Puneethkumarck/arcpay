package com.arcpay.identity.agentidentity.application.controller.owner;

import com.arcpay.identity.agentidentity.api.model.OwnerResponse;
import com.arcpay.identity.agentidentity.api.model.RegisterOwnerRequest;
import com.arcpay.identity.agentidentity.application.controller.owner.mapper.OwnerResponseMapper;
import com.arcpay.identity.agentidentity.domain.owner.OwnerCommandHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/owners")
@RequiredArgsConstructor
@Validated
public class OwnerController {

    private final OwnerCommandHandler ownerCommandHandler;
    private final OwnerResponseMapper ownerResponseMapper;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerResponse register(@Valid @RequestBody RegisterOwnerRequest request) {
        log.info("Owner registration requested");
        var result = ownerCommandHandler.registerOwner(request.email(), request.walletAddress());
        return ownerResponseMapper.toApi(result);
    }
}

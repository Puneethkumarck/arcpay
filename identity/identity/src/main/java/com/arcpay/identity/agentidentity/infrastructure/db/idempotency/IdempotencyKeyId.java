package com.arcpay.identity.agentidentity.infrastructure.db.idempotency;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IdempotencyKeyId implements Serializable {

    private UUID idempotencyKey;

    private UUID ownerId;
}

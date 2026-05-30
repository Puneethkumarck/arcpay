package com.arcpay.settlement.domain.event;

import java.util.UUID;

public sealed interface SettlementEvent permits TransferConfirmed, TransferReverted {

    UUID paymentId();
}

package com.arcpay.compliance.domain.port;

import com.arcpay.compliance.domain.model.ScreeningCheck;

public interface RiskSignalProvider {

    ScreeningCheck provideSignal(String recipientAddress);
}

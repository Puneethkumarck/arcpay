package com.arcpay.compliance.domain.port;

import com.arcpay.compliance.domain.model.SanctionsSet;

public interface SanctionsSetProvider {

    SanctionsSet getCurrentSanctionsSet();
}

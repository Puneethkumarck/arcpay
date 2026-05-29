package com.arcpay.compliance.domain.model;

public enum CheckType {
    SANCTIONS_OFAC,
    SANCTIONS_UN,
    SANCTIONS_EU,
    SANCTIONS_UK,
    WATCHLIST,
    ONCHAIN_INTERACTION,
    ONCHAIN_NOVELTY,
    ONCHAIN_MIXER
}

rootProject.name = "arcpay-platform"

buildCache {
    local { isEnabled = true }
}

// Shared platform modules
include("platform-api")
include("platform-infra")
include("platform-test")

// Identity Service
include("identity:identity-api")
include("identity:identity-client")
include("identity:identity")

// Policy Engine Service
include("policy-engine:policy-engine-api")
include("policy-engine:policy-engine-client")
include("policy-engine:policy-engine")

// Compliance Service
include("compliance:compliance-api")
include("compliance:compliance-client")
include("compliance:compliance")

// Payment Execution Service
include("payment-execution:payment-execution-api")
include("payment-execution:payment-execution-client")
include("payment-execution:payment-execution")

// Settlement Service
include("settlement:settlement-api")
include("settlement:settlement-client")
include("settlement:settlement")

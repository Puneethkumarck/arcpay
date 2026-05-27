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

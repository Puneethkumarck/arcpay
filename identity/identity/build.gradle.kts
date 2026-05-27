plugins {
    id("arcpay.service")
}

dependencies {
    implementation(project(":identity:identity-api"))

    // Service-specific dependencies
    implementation("io.temporal:temporal-spring-boot-starter:1.35.0")
    implementation("org.web3j:core:4.12.3")

    // WireMock for adapter tests
    testImplementation("org.wiremock:wiremock-standalone:3.10.0")
}

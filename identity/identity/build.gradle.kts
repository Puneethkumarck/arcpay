plugins {
    id("arcpay.service")
}

val temporalVersion: String by project
val web3jVersion: String by project
val wiremockVersion: String by project

dependencies {
    implementation(project(":identity:identity-api"))

    // Service-specific dependencies
    implementation("io.temporal:temporal-spring-boot-starter:$temporalVersion")
    implementation("org.web3j:core:$web3jVersion")

    // WireMock for adapter tests
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
}

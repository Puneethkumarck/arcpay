plugins {
    id("arcpay.service")
}

val web3jVersion: String by project
val wiremockVersion: String by project

// Exclude Spring Cloud Stream — not needed until #52 (Outbox Publisher)
configurations.all {
    exclude(group = "org.springframework.cloud", module = "spring-cloud-stream")
    exclude(group = "org.springframework.cloud", module = "spring-cloud-stream-binder-kafka")
    exclude(group = "org.springframework.cloud", module = "spring-cloud-stream-binder-kafka-core")
}

dependencies {
    implementation(project(":policy-engine:policy-engine-api"))
    testFixturesImplementation(project(":policy-engine:policy-engine-api"))

    implementation(project(":identity:identity-client"))

    // OpenFeign for Feign clients and @EnableFeignClients
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // RFC 8785 canonical JSON
    implementation(libs.jcs)

    // web3j for keccak256 hash
    implementation("org.web3j:core:$web3jVersion")

    // Caffeine for API key resolution cache
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation(libs.caffeine)

    // Resilience4j circuit breaker (programmatic, no Spring Boot starter yet for Boot 4)
    implementation(libs.resilience4j.circuitbreaker)

    // ShedLock for scheduled cleanup
    implementation(libs.shedlock.spring)
    implementation(libs.shedlock.provider.jdbc.template)

    // Jackson JSR310 for serialization tests
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // WireMock for adapter tests
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
}

plugins {
    id("arcpay.service")
}

val web3jVersion: String by project
val wiremockVersion: String by project

dependencies {
    implementation(project(":policy-engine:policy-engine-api"))
    testFixturesImplementation(project(":policy-engine:policy-engine-api"))

    implementation(project(":identity:identity-client"))
    // Identity types + Feign exceptions for shared test fixtures
    testFixturesImplementation(project(":identity:identity-client"))
    testFixturesImplementation("io.github.openfeign:feign-core")

    // OpenFeign for Feign clients and @EnableFeignClients
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // RFC 8785 canonical JSON
    implementation(libs.jcs)

    // web3j for keccak256 hash
    implementation("org.web3j:core:$web3jVersion")

    // Caffeine for API key resolution cache
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation(libs.caffeine)

    // Spring Cloud Circuit Breaker (Resilience4j) — wires circuit breaker + time limiter into
    // OpenFeign out of the box; transitively provides resilience4j-circuitbreaker/-timelimiter
    implementation(libs.spring.cloud.circuitbreaker.resilience4j)

    // ShedLock for scheduled cleanup
    implementation(libs.shedlock.spring)
    implementation(libs.shedlock.provider.jdbc.template)

    // Jackson JSR310 for serialization tests
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // WireMock for adapter tests
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")

    // WireMock in shared fixtures so the BusinessTest base class can stub the Identity Service
    testFixturesImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
}

plugins {
    id("arcpay.service")
}

val temporalVersion: String by project
val web3jVersion: String by project
val wiremockVersion: String by project

dependencies {
    implementation(project(":payment-execution:payment-execution-api"))
    testFixturesImplementation(project(":payment-execution:payment-execution-api"))

    implementation(project(":compliance:compliance-api"))
    testFixturesImplementation(project(":compliance:compliance-api"))

    implementation(project(":identity:identity-client"))
    testFixturesImplementation(project(":identity:identity-client"))
    testFixturesImplementation("io.github.openfeign:feign-core")

    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("io.temporal:temporal-spring-boot-starter:$temporalVersion")
    implementation(libs.spring.cloud.circuitbreaker.resilience4j)
    implementation("org.web3j:core:$web3jVersion")
    testFixturesImplementation("org.web3j:core:$web3jVersion")

    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation(libs.caffeine)

    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testFixturesImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
}

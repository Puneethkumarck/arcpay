plugins {
    id("arcpay.service")
}

val temporalVersion: String by project
val web3jVersion: String by project
val wiremockVersion: String by project

dependencies {
    implementation(project(":compliance:compliance-api"))
    testFixturesImplementation(project(":compliance:compliance-api"))

    implementation(project(":identity:identity-client"))
    testFixturesImplementation(project(":identity:identity-client"))
    testFixturesImplementation("io.github.openfeign:feign-core")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-security")
    testFixturesImplementation(libs.resilience4j.circuitbreaker)
    testFixturesImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")

    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("io.temporal:temporal-spring-boot-starter:$temporalVersion")
    implementation("org.web3j:core:$web3jVersion")
    testFixturesImplementation("org.web3j:core:$web3jVersion")

    implementation(libs.spring.cloud.circuitbreaker.resilience4j)

    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation(libs.caffeine)

    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    integrationTestImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    "businessTestImplementation"("org.wiremock:wiremock-standalone:$wiremockVersion")
}

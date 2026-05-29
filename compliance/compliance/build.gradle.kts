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

    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("io.temporal:temporal-spring-boot-starter:$temporalVersion")
    implementation("org.web3j:core:$web3jVersion")

    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.timelimiter)

    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation(libs.caffeine)

    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
}

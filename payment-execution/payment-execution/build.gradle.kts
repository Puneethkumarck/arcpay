plugins {
    id("arcpay.service")
}

val temporalVersion: String by project
val web3jVersion: String by project

dependencies {
    implementation(project(":payment-execution:payment-execution-api"))
    testFixturesImplementation(project(":payment-execution:payment-execution-api"))

    implementation(project(":compliance:compliance-api"))
    testFixturesImplementation(project(":compliance:compliance-api"))

    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("io.temporal:temporal-spring-boot-starter:$temporalVersion")
    implementation(libs.spring.cloud.circuitbreaker.resilience4j)
    implementation("org.web3j:core:$web3jVersion")
    testFixturesImplementation("org.web3j:core:$web3jVersion")
}

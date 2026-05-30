plugins {
    id("arcpay.service")
}

val wiremockVersion: String by project
val web3jVersion: String by project

dependencies {
    implementation(project(":settlement:settlement-api"))
    testFixturesImplementation(project(":settlement:settlement-api"))

    implementation("org.web3j:core:$web3jVersion")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    testFixturesImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    integrationTestImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
}

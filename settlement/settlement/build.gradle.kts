plugins {
    id("arcpay.service")
}

val wiremockVersion: String by project

dependencies {
    implementation(project(":settlement:settlement-api"))
    testFixturesImplementation(project(":settlement:settlement-api"))

    testFixturesImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    integrationTestImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
}

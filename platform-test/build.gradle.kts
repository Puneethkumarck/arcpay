plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    testFixturesApi("org.assertj:assertj-core:3.27.0")
    testFixturesApi("org.mockito:mockito-core")
    testFixturesApi("com.tngtech.archunit:archunit-junit5:1.3.0")
    testFixturesApi("org.testcontainers:postgresql:1.21.4")
    testFixturesApi("org.testcontainers:kafka:1.21.4")
    testFixturesApi("org.testcontainers:junit-jupiter:1.21.4")
    testFixturesApi("org.springframework.boot:spring-boot-starter-test")
    testFixturesApi("org.springframework.boot:spring-boot-starter-webmvc-test")
    testFixturesApi("org.springframework.boot:spring-boot-starter-security-test")
    testFixturesApi("org.springframework.boot:spring-boot-starter-security")
}

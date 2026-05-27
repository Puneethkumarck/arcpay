import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
    `java-test-fixtures`
}

// ---------------------------------------------------------------------------
// BOM imports
// ---------------------------------------------------------------------------
dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.0")
        mavenBom("org.testcontainers:testcontainers-bom:1.21.4")
    }
}

// ---------------------------------------------------------------------------
// Integration test source set
// ---------------------------------------------------------------------------
val integrationTestSourceSet: SourceSet = sourceSets.create("integrationTest") {
    java.srcDir("src/integration-test/java")
    resources.srcDir("src/integration-test/resources")
    compileClasspath += sourceSets.main.get().output + sourceSets.testFixtures.get().output
    runtimeClasspath += sourceSets.main.get().output + sourceSets.testFixtures.get().output
}

configurations {
    named("integrationTestImplementation") { extendsFrom(configurations.testImplementation.get()) }
    named("integrationTestRuntimeOnly") { extendsFrom(configurations.testRuntimeOnly.get()) }
}

tasks.register<Test>("integrationTest") {
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath
    shouldRunAfter(tasks.test)
}

// ---------------------------------------------------------------------------
// Business test source set
// ---------------------------------------------------------------------------
val businessTestSourceSet: SourceSet = sourceSets.create("businessTest") {
    java.srcDir("src/business-test/java")
    resources.srcDir("src/business-test/resources")
    compileClasspath += sourceSets.main.get().output + sourceSets.testFixtures.get().output
    runtimeClasspath += sourceSets.main.get().output + sourceSets.testFixtures.get().output
}

configurations {
    named("businessTestImplementation") { extendsFrom(configurations.testImplementation.get()) }
    named("businessTestRuntimeOnly") { extendsFrom(configurations.testRuntimeOnly.get()) }
}

tasks.register<Test>("businessTest") {
    testClassesDirs = businessTestSourceSet.output.classesDirs
    classpath = businessTestSourceSet.runtimeClasspath
    shouldRunAfter(tasks.named("integrationTest"))
}

// ---------------------------------------------------------------------------
// Common dependencies — every service gets these
// ---------------------------------------------------------------------------
dependencies {
    // Shared runtime infrastructure
    implementation(project(":platform-infra"))

    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Kafka via Spring Cloud Stream
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("org.springframework.cloud:spring-cloud-stream")
    implementation("org.springframework.cloud:spring-cloud-stream-binder-kafka")

    // MapStruct
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

    // Outbox (namastack)
    implementation("io.namastack:namastack-outbox-starter-jdbc:1.6.0")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:11.9.1")

    // UUID
    implementation("com.github.f4b6a3:uuid-creator:6.0.0")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Shared test infrastructure
    testFixturesImplementation(testFixtures(project(":platform-test")))
    testFixturesImplementation("org.assertj:assertj-core")
    testFixturesImplementation("org.mockito:mockito-core")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-web")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testFixturesImplementation("org.testcontainers:junit-jupiter")
    testFixturesImplementation("org.testcontainers:postgresql")
    testFixturesImplementation("org.testcontainers:kafka")

    // Unit tests
    testImplementation(testFixtures(project(":platform-test")))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")

    // Integration tests
    "integrationTestImplementation"("org.springframework.boot:spring-boot-starter-test")
    "integrationTestImplementation"("org.springframework.security:spring-security-test")
    "integrationTestImplementation"("org.springframework.boot:spring-boot-testcontainers")
    "integrationTestImplementation"("org.testcontainers:junit-jupiter")
    "integrationTestImplementation"("org.testcontainers:postgresql")
    "integrationTestImplementation"("org.testcontainers:kafka")
    "integrationTestImplementation"("org.springframework.cloud:spring-cloud-stream-test-binder")

    // Business tests
    "businessTestImplementation"("org.springframework.boot:spring-boot-starter-test")
    "businessTestImplementation"("org.springframework.boot:spring-boot-testcontainers")
    "businessTestImplementation"("org.testcontainers:junit-jupiter")
    "businessTestImplementation"("org.testcontainers:postgresql")
    "businessTestImplementation"("org.testcontainers:kafka")
}

// ---------------------------------------------------------------------------
// MapStruct compiler args
// ---------------------------------------------------------------------------
tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-Amapstruct.defaultComponentModel=spring",
        "-Amapstruct.unmappedTargetPolicy=IGNORE"
    ))
}

// ---------------------------------------------------------------------------
// Test configuration
// ---------------------------------------------------------------------------
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

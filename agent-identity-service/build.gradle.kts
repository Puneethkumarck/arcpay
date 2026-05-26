plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    `java-test-fixtures`
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.bom.get().toString())
        mavenBom(libs.testcontainers.bom.get().toString())
    }
}

dependencies {
    implementation(project(":agent-identity-service-api"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation(libs.spring.cloud.stream)
    implementation(libs.spring.cloud.stream.binder.kafka)

    implementation(libs.namastack.outbox)
    implementation(libs.temporal)
    implementation(libs.web3j)
    implementation(libs.uuid.creator)

    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.wiremock)
    testImplementation(libs.archunit)

    testFixturesImplementation("org.assertj:assertj-core")
    testFixturesImplementation("org.mockito:mockito-core")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-web")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testFixturesImplementation(libs.testcontainers.junit.jupiter)
    testFixturesImplementation(libs.testcontainers.postgresql)
    testFixturesImplementation(libs.testcontainers.kafka)
}

sourceSets {
    create("integrationTest") {
        java.srcDir("src/integration-test/java")
        resources.srcDir("src/integration-test/resources")
        compileClasspath += sourceSets.main.get().output + sourceSets.testFixtures.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.testFixtures.get().output
    }
    create("businessTest") {
        java.srcDir("src/business-test/java")
        resources.srcDir("src/business-test/resources")
        compileClasspath += sourceSets.main.get().output + sourceSets.testFixtures.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.testFixtures.get().output
    }
}

configurations {
    named("integrationTestImplementation") {
        extendsFrom(configurations.testImplementation.get())
    }
    named("integrationTestRuntimeOnly") {
        extendsFrom(configurations.testRuntimeOnly.get())
    }
    named("businessTestImplementation") {
        extendsFrom(configurations.testImplementation.get())
    }
    named("businessTestRuntimeOnly") {
        extendsFrom(configurations.testRuntimeOnly.get())
    }
}

dependencies {
    "integrationTestImplementation"("org.springframework.boot:spring-boot-starter-test")
    "integrationTestImplementation"("org.springframework.boot:spring-boot-testcontainers")
    "integrationTestImplementation"(libs.testcontainers.junit.jupiter)
    "integrationTestImplementation"(libs.testcontainers.postgresql)
    "integrationTestImplementation"(libs.testcontainers.kafka)
    "integrationTestImplementation"(libs.spring.cloud.stream.test.binder)

    "businessTestImplementation"("org.springframework.boot:spring-boot-starter-test")
    "businessTestImplementation"("org.springframework.boot:spring-boot-testcontainers")
    "businessTestImplementation"(libs.testcontainers.junit.jupiter)
    "businessTestImplementation"(libs.testcontainers.postgresql)
    "businessTestImplementation"(libs.testcontainers.kafka)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Test>("integrationTest") {
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter(tasks.test)
}

tasks.register<Test>("businessTest") {
    testClassesDirs = sourceSets["businessTest"].output.classesDirs
    classpath = sourceSets["businessTest"].runtimeClasspath
    shouldRunAfter(tasks.named("integrationTest"))
}

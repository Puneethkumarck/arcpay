plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":platform-api"))
    api("jakarta.validation:jakarta.validation-api")
    api("com.fasterxml.jackson.core:jackson-annotations")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.0")
    testImplementation("org.hibernate.validator:hibernate-validator:9.0.1.Final")
    testImplementation("org.glassfish.expressly:expressly:6.0.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

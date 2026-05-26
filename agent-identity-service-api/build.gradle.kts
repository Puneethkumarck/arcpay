plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(libs.jakarta.validation.api)
    api(libs.jackson.annotations)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.0")
    testImplementation("org.hibernate.validator:hibernate-validator:9.0.1.Final")
    testImplementation("org.glassfish.expressly:expressly:6.0.0")
}

tasks.test {
    useJUnitPlatform()
}

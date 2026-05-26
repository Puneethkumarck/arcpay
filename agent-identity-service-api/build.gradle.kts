plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(libs.jakarta.validation.api)
    api(libs.jackson.annotations)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj.core)
    testImplementation(libs.hibernate.validator)
    testImplementation(libs.expressly)
}

tasks.test {
    useJUnitPlatform()
}

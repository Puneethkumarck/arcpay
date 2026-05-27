plugins {
    `java-library`
    `java-test-fixtures`
}

val junitVersion: String by project
val assertjVersion: String by project
val hibernateValidatorVersion: String by project
val expresslyVersion: String by project

dependencies {
    api(project(":platform-api"))
    api("jakarta.validation:jakarta.validation-api")
    api("com.fasterxml.jackson.core:jackson-annotations")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("org.hibernate.validator:hibernate-validator:$hibernateValidatorVersion")
    testImplementation("org.glassfish.expressly:expressly:$expresslyVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

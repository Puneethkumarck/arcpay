plugins {
    `java-library`
}

val junitVersion: String by project
val assertjVersion: String by project

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

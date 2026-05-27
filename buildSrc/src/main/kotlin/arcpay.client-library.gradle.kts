plugins {
    `java-library`
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

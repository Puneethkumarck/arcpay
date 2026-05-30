plugins {
    `java-library`
}

val namastackVersion: String by project
val wiremockVersion: String by project

dependencies {
    api(project(":platform-api"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.namastack:namastack-outbox-starter-jdbc:$namastackVersion")

    // Feign core for the outbound ServiceAuthFeignInterceptor (RequestInterceptor type).
    // compileOnly: consumers that register the interceptor already bring OpenFeign on the classpath.
    compileOnly("io.github.openfeign:feign-core")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

plugins {
    id("arcpay.client-library")
}

dependencies {
    api(project(":identity:identity-api"))

    api("org.springframework.cloud:spring-cloud-starter-openfeign")
}

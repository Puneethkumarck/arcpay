plugins {
    id("arcpay.client-library")
}

dependencies {
    api(project(":policy-engine:policy-engine-api"))

    api("org.springframework.cloud:spring-cloud-starter-openfeign")
}

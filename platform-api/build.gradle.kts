plugins {
    `java-library`
}

dependencies {
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    api("com.fasterxml.jackson.core:jackson-annotations")
}

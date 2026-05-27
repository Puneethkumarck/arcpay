plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
    java
}

tasks.jar { enabled = false }

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    repositories {
        mavenCentral()
    }

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.3")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.0")
        }
    }

    dependencies {
        "compileOnly"("org.projectlombok:lombok:1.18.46")
        "annotationProcessor"("org.projectlombok:lombok:1.18.46")
        "testCompileOnly"("org.projectlombok:lombok:1.18.46")
        "testAnnotationProcessor"("org.projectlombok:lombok:1.18.46")
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

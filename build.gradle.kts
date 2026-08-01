plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "dev.inboxpilot"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        // Pinned so the build is reproducible regardless of the developer's
        // default JDK. Versions live in gradle/libs.versions.toml.
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.validation)

    // Generates IDE metadata for the @ConfigurationProperties types, so
    // application.yml keys get completion and inline documentation.
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.assertj.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

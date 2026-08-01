import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "dev.inboxpilot"
version = providers.gradleProperty("releaseVersion")
    .orElse("0.1.0-SNAPSHOT")
    .get()

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

    // Desktop OAuth for Gmail (issue #9): the loopback authorization-code flow
    // and the token cache it persists to the configured token store.
    implementation(libs.google.oauth.client.jetty)
    implementation(libs.google.api.client)
    implementation(libs.google.api.services.gmail)

    // Generates IDE metadata for the @ConfigurationProperties types, so
    // application.yml keys get completion and inline documentation.
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.assertj.core)
    testImplementation(libs.archunit.junit5)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

val bootJarTask = tasks.named<BootJar>("bootJar")
val distributionContents: CopySpec.() -> Unit = {
    from(bootJarTask) {
        into("lib")
        rename { "inboxpilot.jar" }
    }
    from("packaging/bin") {
        into("bin")
        filePermissions { unix("rwxr-xr-x") }
    }
    from("config/application-example.yml") {
        into("config")
    }
    from("doc") {
        into("doc")
    }
    from("README.md")
}

tasks.register<Zip>("distributionZip") {
    group = "distribution"
    description = "Builds a runnable InboxPilot ZIP distribution."
    dependsOn(bootJarTask)
    archiveBaseName.set(project.name)
    archiveVersion.set(project.version.toString())
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    into("${project.name}-${project.version}", distributionContents)
}

tasks.register<Tar>("distributionTar") {
    group = "distribution"
    description = "Builds a runnable InboxPilot compressed TAR distribution."
    dependsOn(bootJarTask)
    archiveBaseName.set(project.name)
    archiveVersion.set(project.version.toString())
    archiveExtension.set("tar.gz")
    compression = Compression.GZIP
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    into("${project.name}-${project.version}", distributionContents)
}

tasks.named("assemble") {
    dependsOn("distributionZip", "distributionTar")
}

tasks.named<BootBuildImage>("bootBuildImage") {
    imageName.set("ghcr.io/kekukhvy/inboxpilot:${project.version}")
    environment.set(mapOf("BP_JVM_VERSION" to libs.versions.java.get()))
}

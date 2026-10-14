plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "id.andreasmlbngaol"
version = "0.0.1-SNAPSHOT"
description = "KrA server — AI agent + CSP for KRS assistance"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web / validation / security / ops
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.actuator)

    // Persistence (JPA + Hibernate), migrations, driver
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.flyway)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)

    // Auth
    implementation(libs.java.jwt)

    // Kotlin / JSON
    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlinx.serialization.json)
    // Force kotlinx.coroutines 1.11.0 for ktor 3.6.0 compatibility
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0")

    // AI agent
    implementation(libs.koog.agents)
    implementation(libs.koog.agents.additions)
    implementation(libs.koog.http.client.ktor)
    // Force ktor 3.6.0 to fix Content-Length mismatch bug in 3.3.3 (koog 1.3.0 pins 3.3.3)
    implementation("io.ktor:ktor-client-core:3.6.0")
    implementation("io.ktor:ktor-client-apache5:3.6.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.6.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.6.0")
    implementation("io.ktor:ktor-http:3.6.0")
    implementation("io.ktor:ktor-utils:3.6.0")
    implementation("io.ktor:ktor-io:3.6.0")
    implementation("io.ktor:ktor-network:3.6.0")
    implementation("io.ktor:ktor-client-core-jvm:3.6.0")
    implementation("io.ktor:ktor-client-apache5-jvm:3.6.0")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:3.6.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.6.0")
    implementation("io.ktor:ktor-http-jvm:3.6.0")
    implementation("io.ktor:ktor-utils-jvm:3.6.0")
    implementation("io.ktor:ktor-io-jvm:3.6.0")
    implementation("io.ktor:ktor-network-jvm:3.6.0")

    // OpenAPI contract (consumed by the frontend TS codegen)
    implementation(libs.springdoc.openapi)

    // Test
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junit.platform.launcher)
    // In-memory DB so the context loads in tests without a real PostgreSQL.
    testRuntimeOnly(libs.h2)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

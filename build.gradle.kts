plugins {
    kotlin("jvm") version "2.0.0"
    `maven-publish`
}

group = "io.4rc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

object Versions {
    val postgres = "42.5.1"
    val jackson = "2.15.2"
    val jooq = "3.18.6"
    val kotlinWrappers = "1.0.0-pre.661"
    val flyway = "9.20.1"
    val javalin = "6.1.3"
}

dependencies {
    api(kotlin("stdlib"))
    api("org.jetbrains.kotlinx:kotlinx-html:0.9.1")
    api("org.postgresql:postgresql:${Versions.postgres}")
    api("io.javalin:javalin:${Versions.javalin}")
    api("org.jooq:jooq:${Versions.jooq}")
    api("org.jooq:jooq-kotlin:${Versions.jooq}")
    api("org.flywaydb:flyway-core:${Versions.flyway}")

    api("com.auth0:java-jwt:4.4.0")
    api("com.zaxxer:HikariCP:2.6.1")
    api("ch.qos.logback:logback-classic:1.4.6")
    api("ch.qos.logback:logback-core:1.4.6")

    api("at.favre.lib:bcrypt:0.10.2")
    api("com.fasterxml.jackson.core:jackson-core:${Versions.jackson}")
    api("com.fasterxml.jackson.core:jackson-databind:${Versions.jackson}")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${Versions.jackson}")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jackson}")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${Versions.jackson}")
    api("io.github.cdimascio:dotenv-kotlin:6.4.1")
    api("com.squareup.okhttp3:okhttp:4.12.0")

    api("com.google.guava:guava:33.0.0-jre")
    api("dev.misfitlabs.kotlinguice4:kotlin-guice:3.0.0")
    api("com.postmarkapp:postmark:1.11.1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}
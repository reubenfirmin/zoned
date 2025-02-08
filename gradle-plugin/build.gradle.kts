plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.4rc"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(gradleApi())
    implementation(kotlin("gradle-plugin"))
    implementation("com.github.node-gradle:gradle-node-plugin:${Versions.gradleNodePlugin}")
    implementation("org.jooq:jooq:${Versions.jooq}")
    implementation("org.jooq:jooq-meta:${Versions.jooq}")
    implementation("org.jooq:jooq-codegen:${Versions.jooq}")
    implementation("org.flywaydb:flyway-core:${Versions.flyway}")
    implementation("com.zaxxer:HikariCP:${Versions.hikari}")
    implementation("org.xerial:sqlite-jdbc:${Versions.sqlite}")
    implementation("io.github.cdimascio:dotenv-kotlin:${Versions.dotenv}")
}

gradlePlugin {
    plugins {
        create("zonedPlugin") {
            id = "io.4rc.zoned.plugin"
            implementationClass = "zoned.gradle.ZonedPlugin"
        }
    }
}
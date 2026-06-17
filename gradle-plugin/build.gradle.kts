plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("gradle-plugin"))
    implementation("com.github.node-gradle:gradle-node-plugin:${Versions.gradleNodePlugin}")
    implementation("org.jooq:jooq:${Versions.jooq}")
    implementation("org.jooq:jooq-meta:${Versions.jooq}")
    implementation("org.jooq:jooq-codegen:${Versions.jooq}")
    implementation("org.flywaydb:flyway-core:${Versions.flyway}")
    // Flyway 10+ moved database-specific support out of flyway-core into per-database modules.
    implementation("org.flywaydb:flyway-database-postgresql:${Versions.flyway}")
    implementation("com.zaxxer:HikariCP:${Versions.hikari}")
    implementation("org.xerial:sqlite-jdbc:${Versions.sqlite}")
    implementation("org.postgresql:postgresql:${Versions.postgres}")
    implementation("io.github.cdimascio:dotenv-kotlin:${Versions.dotenv}")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("zonedPlugin") {
            id = "io.4rc.zoned.plugin"
            implementationClass = "zoned.gradle.ZonedPlugin"
        }
    }
}
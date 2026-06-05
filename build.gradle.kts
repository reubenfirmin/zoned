plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("maven-publish")
    id("io.4rc.zoned.plugin") version "1.0-SNAPSHOT"
}

allprojects {
    group = "io.4rc"
    version = "1.0-SNAPSHOT"
}

kotlin {
    jvmToolchain(21)

    jvm {
    }

    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        nodejs()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlin("stdlib"))
                api("org.jetbrains.kotlinx:kotlinx-html:0.12.0-web")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            }
        }

        val jvmMain by getting {
            kotlin.srcDir("${project.buildDir}/generated/kotlin")

            dependencies {
                api("org.postgresql:postgresql:${Versions.postgres}")
                                                    api("org.xerial:sqlite-jdbc:${Versions.sqlite}")
                api("io.javalin:javalin:${Versions.javalin}")
                api("org.jooq:jooq:${Versions.jooq}")
                api("org.jooq:jooq-kotlin:${Versions.jooq}")
                api("org.flywaydb:flyway-core:${Versions.flyway}")
                api("net.bytebuddy:byte-buddy:1.18.10")
                api("com.auth0:java-jwt:4.5.2")
                api("com.zaxxer:HikariCP:${Versions.hikari}")
                api("ch.qos.logback:logback-classic:1.5.34")
                api("ch.qos.logback:logback-core:1.5.34")
                api("at.favre.lib:bcrypt:0.10.2")
                api("com.fasterxml.jackson.core:jackson-core:${Versions.jackson}")
                api("com.fasterxml.jackson.core:jackson-databind:${Versions.jackson}")
                api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${Versions.jackson}")
                api("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jackson}")
                api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${Versions.jackson}")
                api("io.github.cdimascio:dotenv-kotlin:${Versions.dotenv}")
                api("com.squareup.okhttp3:okhttp:5.3.2")
                api("com.google.guava:guava:33.6.0-jre")
                api("dev.misfitlabs.kotlinguice4:kotlin-guice:3.0.0")
                api("com.postmarkapp:postmark:1.13.0")
                api("org.jooq:jooq-meta:${Versions.jooq}")
                api("org.jooq:jooq-codegen:${Versions.jooq}")
            }
        }

        val jsMain by getting {
            kotlin.srcDir("${project.buildDir}/generated/kotlin-js")

            dependencies {
                api(project.dependencies.platform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:${Versions.kotlinWrappers}"))
                api("org.jetbrains.kotlin-wrappers:kotlin-js")
                api("org.jetbrains.kotlin-wrappers:kotlin-browser")
                api("org.jetbrains.kotlin-wrappers:kotlin-css")
                api("org.jetbrains.kotlinx:kotlinx-html-js:0.12.0-web")

                api(npm("tailwindcss", "4.3.0"))
                api(npm("@tailwindcss/cli", "4.3.0"))
                api(npm("sortablejs", "1.15.7"))
                api(npm("flowbite", "4.0.2"))
                api(npm("htmx.org", "2.0.10"))
                api(npm("ace-builds", "1.44.0"))
                api(npm("file-loader", "6.2.0"))
                api(npm("path-browserify", "1.0.1"))
                api(npm("crypto-browserify", "3.12.1"))
                api(npm("stream-browserify", "3.0.0"))
                api(npm("buffer", "6.0.3"))
                api(npm("dragula", "3.7.3"))
                api(npm("file-saver", "2.0.5"))
                api(npm("browser-fs-access", "0.38.0"))
                api(npm("html-to-image", "1.11.13"))
                api(npm("microlight", "0.0.7"))
                api(npm("leaflet", "1.9.4"))
                api(npm("nomnoml", "1.7.0"))
                api(npm("svgmap", "2.20.1"))
                api(npm("@popperjs/core", "2.11.8"))
                api(npm("apexcharts", "5.14.0"))
                api(npm("prismjs", "1.30.0"))
                api(npm("tributejs", "5.1.3"))
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jsoup:jsoup:1.22.2")
                implementation("com.squareup.okhttp3:okhttp:5.3.2")
            }
        }
    }
}
//
//tasks.named("compileKotlinJs", Kotlin2JsCompile::class.java) {
//    compilerOptions {
//        moduleKind.set(JsModuleKind.MODULE_COMMONJS)
//    }
//}
//
//tasks {
//    withType<Jar> {
//        duplicatesStrategy = DuplicatesStrategy.INCLUDE
//    }
//}
//
//publishing {
//    publications.all {
//        // Suppress the automatic plugin publication
//        if (name == "pluginMaven") {
//            tasks.withType<AbstractPublishToMaven>()
//                .matching { it.publication == this }
//                .configureEach { enabled = false }
//        }
//    }
//    repositories {
//        mavenLocal()
//    }
//}
package zoned.gradle.generation

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.*
import zoned.gradle.Config
import zoned.gradle.DatabaseSetup
import java.io.File

open class JooqGenerator : DefaultTask() {

    @TaskAction
    fun jooqGenerate() {
        val config = DatabaseSetup(logger).config
        val sourceDir = findSourceDirectory()
        val modelPackage = findModelPackage(sourceDir)

        GenerationTool.generate(
            Configuration()
                .withLogging(Logging.DEBUG)
                .withJdbc(jdbc(config))
                .withGenerator(
                    Generator()
                        .withDatabase(database(config))
                        .withName("zoned.gradle.generation.EntityKotlinGenerator")
                        .withGenerate(
                            Generate()
                                .withPojos(true)
                                .withKotlinNotNullPojoAttributes(true)
                                .withKotlinNotNullInterfaceAttributes(true)
                                .withKotlinNotNullRecordAttributes(true)
                                .withPojosAsKotlinDataClasses(true)
                                // jooq bug - these should be default false for data classes
                                // (https://github.com/jOOQ/jOOQ/issues/10917)
                                .withPojosEqualsAndHashCode(false)
                                .withPojosToString(false)
                                .withImmutablePojos(true)
                        )
                        .withStrategy(Strategy().apply {
                            withName("zoned.gradle.generation.EntityGenerationStrategy")
                        })
                        .withTarget(org.jooq.meta.jaxb.Target()
                            .withPackageName("${modelPackage}.jooq")
                            .withDirectory(sourceDir)
                        )
                )
                .withOnError(OnError.FAIL)
                .withLogging(Logging.DEBUG)
        )

        // now clean up the record classes
        RecordUpdater(project).updateRecords()
    }

    private fun jdbc(config: Config): Jdbc {
        return if (config.dbPath != null) {
            val dbFile = project.file(config.dbPath).absolutePath

            Jdbc()
                .withDriver("org.sqlite.JDBC")
                .withUrl("jdbc:sqlite:${dbFile}")
        } else {
            Jdbc()
                .withDriver("org.postgresql.Driver")
                .withUrl(config.dbUrl)
                .withUser(config.dbUser)
                .withPassword(config.dbPass)
        }
    }

    private fun suppressor(config: Config): String {
        return if (config.dbPath != null) {
            "zoned.gradle.generation.DefaultSuppressingSqlliteConfig"
        } else {
            "zoned.gradle.generation.DefaultSuppressingPostgresConfig"
        }
    }

    private fun database(config: Config): Database {
        return if (config.dbPath != null) {
            logger.info("Introspecting sqllite database")
            Database()
                .withName(suppressor(config))
                .withIncludes(".*")
        } else {
            logger.info("Introspecting postgres database")
            Database()
                .withName(suppressor(config))
                .withIncludes(".*")
                .withInputSchema("public")
        }
    }

    private fun findSourceDirectory(): String {
        val kotlinDir = project.file("src/main/kotlin")
        val jvmMainDir = project.file("src/jvmMain/kotlin")

        return when {
            kotlinDir.exists() -> kotlinDir.absolutePath
            jvmMainDir.exists() -> jvmMainDir.absolutePath
            else -> throw IllegalStateException("Neither src/main/kotlin nor src/jvmMain/kotlin directory found")
        }
    }

    private fun findModelPackage(sourceDir: String): String {
        val sourceDirFile = File(sourceDir)
        val modelDir = sourceDirFile.walkTopDown()
            .filter { it.isDirectory && it.name == "model" }
            .firstOrNull()
            ?: throw IllegalStateException("No model directory found in $sourceDir")

        return modelDir.toRelativeString(sourceDirFile)
            .replace(File.separator, ".")
            .trimStart('.')
    }
}


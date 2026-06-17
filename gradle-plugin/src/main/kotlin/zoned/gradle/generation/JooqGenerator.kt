package zoned.gradle.generation

import org.gradle.api.DefaultTask
import org.gradle.work.DisableCachingByDefault
import org.gradle.api.tasks.TaskAction
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.*
import zoned.gradle.Config
import zoned.gradle.DatabaseSetup
import zoned.gradle.ZonedExtension
import java.io.File

@DisableCachingByDefault(because = "Generates jOOQ sources from a live database")
open class JooqGenerator : DefaultTask() {

    @TaskAction
    fun jooqGenerate() {
        val config = DatabaseSetup(logger).config
        val sourceDir = findSourceDirectory()
        val modelPackage = findModelPackage(sourceDir)
        val extension = project.extensions.findByType(ZonedExtension::class.java)

        GenerationTool.generate(
            buildConfiguration(
                jdbc = jdbc(config),
                suppressorDatabase = suppressor(config),
                isSqlite = config.dbPath != null,
                packageName = "${modelPackage}.jooq",
                targetDirectory = sourceDir,
                enumMappings = extension?.enumMappings ?: emptyList(),
                forcedTypes = extension?.forcedTypes ?: emptyList()
            )
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

    companion object {
        /**
         * Builds the jOOQ [Configuration] that drives entity generation. Extracted from the Gradle
         * task so it can be exercised directly by tests against a throwaway database, guaranteeing
         * the task and the tests share one code path.
         */
        fun buildConfiguration(
            jdbc: Jdbc,
            suppressorDatabase: String,
            isSqlite: Boolean,
            packageName: String,
            targetDirectory: String,
            enumMappings: List<Pair<String, String>> = emptyList(),
            forcedTypes: List<Triple<String, String, String>> = emptyList()
        ): Configuration {
            val database = Database()
                .withName(suppressorDatabase)
                .withIncludes(".*")
                .apply { if (!isSqlite) withInputSchema("public") }

            // SQLite has no uuid/timestamptz/enum types — everything is TEXT/NUMERIC. Force the rich
            // Kotlin types back via converters so the generated model matches the Postgres-era types.
            if (isSqlite) {
                // jOOQ maps uuid -> UUID, date -> LocalDate, numeric -> BigDecimal, boolean -> Boolean
                // natively for SQLite. Only timestamptz (-> generic OTHER) and enums (-> CLOB/String)
                // need forced converters.
                val forced = mutableListOf<ForcedType>()
                forced += ForcedType()
                    .withIncludeTypes("(?i)timestamptz|timestamp|datetime")
                    .withUserType("java.time.OffsetDateTime")
                    .withConverter("zoned.framework.db.OffsetDateTimeConverter")
                enumMappings.forEach { (column, enumFqn) ->
                    forced += ForcedType()
                        .withIncludeExpression("(?i).*\\.$column")
                        .withUserType(enumFqn)
                        .withConverter("org.jooq.impl.EnumConverter<kotlin.String, $enumFqn>(kotlin.String::class.java, $enumFqn::class.java)")
                }
                forcedTypes.forEach { (column, userType, converter) ->
                    forced += ForcedType()
                        .withIncludeExpression("(?i).*\\.$column")
                        .withUserType(userType)
                        .withConverter(converter)
                }
                database.withForcedTypes(forced)
            }

            return Configuration()
                .withLogging(Logging.DEBUG)
                .withJdbc(jdbc)
                .withGenerator(
                    Generator()
                        .withDatabase(database)
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
                            .withPackageName(packageName)
                            .withDirectory(targetDirectory)
                        )
                )
                .withOnError(OnError.FAIL)
                .withLogging(Logging.DEBUG)
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


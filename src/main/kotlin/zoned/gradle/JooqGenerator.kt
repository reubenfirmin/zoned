package zoned.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.*
import java.io.File

open class JooqGenerator : DefaultTask() {

    @TaskAction
    fun jooqGenerate() {
        val config = DatabaseSetup().config
        val sourceDir = findSourceDirectory()
        val modelPackage = findModelPackage(sourceDir)

        GenerationTool.generate(
            Configuration()
                .withLogging(Logging.DEBUG)
                .withJdbc(
                    Jdbc()
                    .withDriver("org.postgresql.Driver")
                    .withUrl(config.dbUrl)
                    .withUser(config.dbUser)
                    .withPassword(config.dbPass)
                )
                .withGenerator(
                    Generator().withDatabase(
                        Database()
                        .withName("zoned.gradle.DefaultSuppressingPostgresConfig")
                        .withIncludes(".*")
                        .withInputSchema("public")
                    )
                        .withName("org.jooq.codegen.KotlinGenerator")
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
                            withName("zoned.gradle.RenamingStrategy")
                        })
                        .withTarget(org.jooq.meta.jaxb.Target()
                            .withPackageName("${modelPackage}.jooq")
                            .withDirectory(sourceDir)
                        )
                )
                .withOnError(OnError.FAIL)
                .withLogging(Logging.DEBUG)
        )
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


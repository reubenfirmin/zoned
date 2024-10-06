package zoned.gradle

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.Dotenv
import org.flywaydb.core.Flyway
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.configurationcache.extensions.capitalized
import org.jooq.codegen.DefaultGeneratorStrategy
import org.jooq.codegen.GenerationTool
import org.jooq.codegen.GeneratorStrategy.Mode
import org.jooq.codegen.GeneratorStrategy.Mode.*
import org.jooq.meta.Definition
import org.jooq.meta.TableDefinition
import org.jooq.meta.jaxb.*
import java.io.File

open class DatabaseSetup() : DefaultTask() {

    private val config = Config()

    @TaskAction
    fun clean() {
        println("WARNING - cleaning")
        getFlyway(false).clean()
    }

    @TaskAction
    fun migrate() {
        // TODO support snapshot and rollback
        getFlyway(true).migrate()
    }

    @TaskAction
    fun jooqGenerate() {
        val sourceDir = findSourceDirectory()
        val modelPackage = findModelPackage(sourceDir)

        GenerationTool.generate(
            Configuration()
                .withLogging(Logging.DEBUG)
                .withJdbc(Jdbc()
                    .withDriver("org.postgresql.Driver")
                    .withUrl(config.dbUrl)
                    .withUser(config.dbUser)
                    .withPassword(config.dbPass)
                )
                .withGenerator(
                    Generator().withDatabase(Database()
                        .withName("zoned.gradle.DefaultSuppressingPostgresConfig")
                        .withIncludes(".*")
                        .withInputSchema("public")
                    )
                    .withName("org.jooq.codegen.KotlinGenerator")
                    .withGenerate(Generate()
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

    private fun getFlyway(cleanDisabled: Boolean): Flyway {
        val dataSource = getDataSource()

        val config = org.flywaydb.core.api.configuration.FluentConfiguration()
            .dataSource(dataSource)
            .cleanDisabled(cleanDisabled)
            // apparently buildSrc doesn't get the same classpath? whatever
            .locations("filesystem:${project.rootDir.absolutePath}/src/jvmMain/resources/db/migration")

        return Flyway(config)
    }

    private fun getDataSource(): HikariDataSource {
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = config.dbUrl
        hikariConfig.username = config.dbUser
        hikariConfig.password = config.dbPass
        hikariConfig.driverClassName = "org.postgresql.Driver"
        hikariConfig.idleTimeout = 2
        hikariConfig.maximumPoolSize = 2
        return HikariDataSource(hikariConfig)
    }
}

val exceptions = setOf<String>()

fun String.singularize() = if (exceptions.contains(this)) { this } else { this.removeSuffix("s") }

class RenamingStrategy() : DefaultGeneratorStrategy() {

    override fun getJavaClassImplements(definition: Definition?, mode: Mode?): MutableList<String> {
        val defs = super.getJavaClassImplements(definition, mode)
        val addl = when (mode) {
            RECORD ->  "zoned.framework.db.Record"
            POJO -> "zoned.framework.db.Entity"
            DEFAULT -> {
                if (definition is TableDefinition) {
                    "zoned.framework.db.EntityTable"
                } else {
                    null
                }
            }
            else -> null
        }
        if (addl != null) {
            return (defs + addl).toMutableList()
        }
        return defs
    }

    override fun getJavaClassName(definition: Definition?, mode: Mode?): String {
        val defaultName = super.getJavaClassName(definition, mode)
        return defaultName.singularize()
    }

    override fun getJavaIdentifier(definition: Definition): String {
        val identifier = super.getJavaIdentifier(definition)
        return identifier.split('_')
            .joinToString("") { it.lowercase().capitalized() }
    }
}

val dotenv = Dotenv.load()

// TODO this is similar but not identical to the one in Configurator
data class Config(
    val dbUser: String = dotenv["DB_USER"],
    val dbPass: String = dotenv["DB_PASS"],

    val dbHost: String = dotenv["DB_HOST"],
    val dbPort: String = dotenv["DB_PORT"],
    val dbName: String = dotenv["DB_NAME"],

    val dbUrl: String = "jdbc:postgresql://${dbHost}:${dbPort}/${dbName}"
)
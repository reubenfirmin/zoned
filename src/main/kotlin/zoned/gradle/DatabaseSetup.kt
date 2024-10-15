package zoned.gradle

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.Dotenv
import org.flywaydb.core.Flyway
import org.gradle.api.Project

class DatabaseSetup() {

    val config = Config()

    fun getFlyway(project: Project, cleanDisabled: Boolean): Flyway {
        val dataSource = getDataSource()

        val config = org.flywaydb.core.api.configuration.FluentConfiguration()
            .dataSource(dataSource)
            .cleanDisabled(cleanDisabled)
            // TODO probably a direct way to get this
            // TODO will this work with jar deploys??
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

val dotenv = Dotenv.load()

// TODO this is similar but not identical to the one in Configurator
data class Config(
    val dbUser: String = dotenv["DB_USER"]!!,
    val dbPass: String = dotenv["DB_PASS"]!!,

    val dbHost: String = dotenv["DB_HOST"]!!,
    val dbPort: String = dotenv["DB_PORT"]!!,
    val dbName: String = dotenv["DB_NAME"]!!,

    val dbUrl: String = "jdbc:postgresql://${dbHost}:${dbPort}/${dbName}"
)
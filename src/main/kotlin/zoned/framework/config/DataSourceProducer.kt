package zoned.framework.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import javax.sql.DataSource

class DataSourceProducer {

    companion object {

        fun providePostgresDataSource(config: DBConfig): DataSource {
            val hikariConfig = HikariConfig()
            hikariConfig.jdbcUrl = config.dbUrl()
            hikariConfig.username = config.dbUser
            hikariConfig.password = config.dbPass
            hikariConfig.minimumIdle = 1
            hikariConfig.maximumPoolSize = 10
            hikariConfig.driverClassName = "org.postgresql.Driver"
            hikariConfig.addDataSourceProperty(
                "sslmode",
                "disable"
            ) // TODO needed for fly

            return HikariDataSource(hikariConfig)
        }

        fun provideSqlLitePooledDataSource(config: SQLliteDBConfig): DataSource {
            val hikariConfig = HikariConfig()
            // XXX this wants to be an absolute path
            hikariConfig.jdbcUrl = "jdbc:sqlite:${File(config.dbPath).absolutePath}"
            hikariConfig.minimumIdle = 1
            hikariConfig.maximumPoolSize = 10
            hikariConfig.driverClassName = "org.sqlite.JDBC"

            hikariConfig.addDataSourceProperty("cache", "shared")
            hikariConfig.addDataSourceProperty("foreign_keys", "on")
            hikariConfig.addDataSourceProperty("journal_mode", "WAL")

            return HikariDataSource(hikariConfig)
        }

        fun provideSqlLiteDirectDataSource(config: SQLliteDBConfig): DataSource {
            val ds = org.sqlite.SQLiteDataSource()
            ds.url = "jdbc:sqlite:${config.dbPath}?foreign_keys=on&journal_mode=WAL"
            return ds
        }

    }
}
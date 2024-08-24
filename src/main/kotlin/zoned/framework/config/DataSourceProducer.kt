package zoned.framework.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource

class DataSourceProducer {

    companion object {
        fun provideDataSource(config: Config): DataSource {
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
            ) // TODO needed for fly; probably remove for supabase

            return HikariDataSource(hikariConfig)
        }
    }
}
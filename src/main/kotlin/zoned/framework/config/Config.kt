package zoned.framework.config

// TODO not framework
data class Config(
    val dbUser: String = Configurator.env("DB_USER"),
    val dbPass: String = Configurator.env("DB_PASS"),
    val dbHost: String = Configurator.env("DB_HOST"),
    val dbPort: String = Configurator.env("DB_PORT"),
    val dbName: String = Configurator.env("DB_NAME"),
    val jwtSecret: String = Configurator.env("JWT_SECRET"),
    val exchKey: String = Configurator.env("EXCH_API_KEY"),
    val imageKey: String = Configurator.env("IMAGE_KEY"),
    val stripeKey: String = Configurator.env("STRIPE_KEY"),
    val postmarkSecret: String = Configurator.env("POSTMARK_SECRET"),
    val baseUrl: String = Configurator.env("BASE_URL"),
    val admins: String = Configurator.env("ADMINS"),
    val environment: String = Configurator.env("ENV"),
    val callbackUrl: String = Configurator.env("CALLBACK_URL"),
) {
    fun dbUrl() = "jdbc:postgresql://${dbHost}:${dbPort}/${dbName}"

    fun env() = Environment.valueOf(environment)
}

enum class Environment {
    PRODUCTION,
    STAGING,
    DEVELOPMENT
}
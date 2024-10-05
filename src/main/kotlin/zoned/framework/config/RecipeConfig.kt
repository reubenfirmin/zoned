package zoned.framework.config

// TODO not framework
interface RecipeConfig : Config, DBConfig, EnvironmentConfig {

    @Env("JWT_SECRET") val jwtSecret: String
    @Env("EXCH_API_KEY") val exchKey: String
    @Env("IMAGE_KEY") val imageKey: String
    @Env("STRIPE_KEY") val stripeKey: String
    @Env("POSTMARK_SECRET") val postmarkSecret: String
    @Env("BASE_URL") val baseUrl: String
    @Env("ADMINS") val admins: String
    @Env("CALLBACK_URL") val callbackUrl: String
}

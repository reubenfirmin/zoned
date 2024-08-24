package zoned.framework.config

import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.DotenvException

class Configurator {

    companion object {
        private val dotenv by lazy { Dotenv.load() }

        /**
         * Look in environment first (e.g. staging/prod), then look for .env file on the classpath, then
         * give up and complain
         */
        fun env(key: String) = try {
            System.getenv(key) ?: dotenv[key] ?: throw Exception("$key not found")
        } catch (e: DotenvException) {
            throw Exception("$key not found when initializing dotenv", e)
        }

        fun load(): Config = Config()
    }
}
package zoned.framework.config

import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.DotenvException
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@Target(AnnotationTarget.PROPERTY)
annotation class Env(val key: String)

enum class Environment {
    PRODUCTION,
    STAGING,
    DEVELOPMENT
}

interface Config

interface DBConfig: Config {
    @Env("DB_USER") val dbUser: String
    @Env("DB_PASS") val dbPass: String
    @Env("DB_HOST") val dbHost: String
    @Env("DB_PORT") val dbPort: String
    @Env("DB_NAME") val dbName: String

}
fun DBConfig.dbUrl() = "jdbc:postgresql://$dbHost:$dbPort/$dbName"

interface EnvironmentConfig: Config {
    @Env("ENV") val environment: String
}
fun EnvironmentConfig.env(): Environment {
    try {
        return Environment.valueOf(environment)
    } catch (e: Exception) {
        throw Exception("$environment is not a valid Environment")
    }
}

class Configurator {

    companion object {
        private val dotenv by lazy { Dotenv.load() }

        /**
         * Look in environment first (e.g. staging/prod), then look for .env file on the classpath, then
         * give up and complain
         */
        private fun env(key: String): String = System.getenv(key)
            ?: dotenv[key]
            ?: throw Exception("$key not found in environment or .env file")

        inline fun <reified T : Config> load(overrides: Map<String, Any> = mapOf()): T = load(T::class, overrides)

        fun <T : Config> load(clazz: KClass<T>, overrides: Map<String, Any> = mapOf()): T {
            val envValues = mutableMapOf<String, Any>()
            val interfaceMethods = clazz.memberFunctions.associateBy { it.name }

            // Populate the map with environment values
            clazz.memberProperties.forEach { prop ->
                prop.findAnnotation<Env>()?.let { env ->
                    envValues[prop.name] = env(env.key)
                }
            }

            @Suppress("UNCHECKED_CAST")
            return Proxy.newProxyInstance(
                clazz.java.classLoader,
                arrayOf(clazz.java)
            ) { _, method, args ->
                // XXX hacky, cleanup
                val propertyName = method.name.let { name ->
                    if (name.startsWith("get")) {
                        name.substring(3,4).lowercase() + name.substring(4)
                    } else {
                        name
                    }
                }

                when {
                    overrides.containsKey(method.name) -> {
                        overrides[method.name]
                    }
                    // note: these have to be extension functions
                    interfaceMethods.containsKey(method.name) -> {
                        val kotlinFunction = interfaceMethods[method.name]
                        kotlinFunction?.call(null, *args.orEmpty())
                            ?: throw IllegalStateException("Method not found: ${method.name}")
                    }
                    envValues.containsKey(propertyName) -> envValues[propertyName]
                    else -> throw IllegalStateException("No value found for property: ${method.name} / $propertyName")
                }
            } as T
        }
    }
}
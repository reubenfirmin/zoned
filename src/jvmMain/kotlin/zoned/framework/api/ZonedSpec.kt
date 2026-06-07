package zoned.framework.api

import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.staticfiles.Location
import org.slf4j.LoggerFactory
import zoned.framework.di.KModule
import zoned.framework.di.set

data class StaticFileEntry(
    val hostedPath: String,
    val directory: String,
    val location: Location
)

class ZonedSpec internal constructor() {

    private val log = LoggerFactory.getLogger(ZonedSpec::class.java)
    private val accessLogger = LoggerFactory.getLogger("javalin.access")

    private var authHandler: Handler? = null
    private val apis = mutableListOf<Api>()
    private val staticFileEntries = mutableListOf<StaticFileEntry>()
    private var corsAnyHost = true
    private var accessLogEnabled = true
    private var exceptionHandler: ((Exception, Context) -> Unit)? = null
    private val errorHandlers = mutableMapOf<Int, Handler>()
    private var rawConfig: ((JavalinConfig) -> Unit)? = null

    fun auth(handler: Handler) { authHandler = handler }

    fun bindings(block: KModule.() -> Unit) { set(block) }

    fun apis(vararg resources: Api) { apis.addAll(resources) }

    fun staticFiles(hostedPath: String, directory: String, location: Location = Location.EXTERNAL) {
        staticFileEntries.add(StaticFileEntry(hostedPath, directory, location))
    }

    fun cors(anyHost: Boolean) { corsAnyHost = anyHost }
    fun accessLog(enabled: Boolean) { accessLogEnabled = enabled }
    fun onException(handler: (Exception, Context) -> Unit) { exceptionHandler = handler }
    fun onError(code: Int, handler: Handler) { errorHandlers[code] = handler }
    fun onNotFound(handler: Handler) { errorHandlers[404] = handler }
    fun javalin(block: (JavalinConfig) -> Unit) { rawConfig = block }

    internal fun applyTo(config: JavalinConfig) {
        staticFileEntries.forEach { entry ->
            config.staticFiles.add {
                it.hostedPath = entry.hostedPath
                it.directory = entry.directory
                it.location = entry.location
            }
        }

        if (corsAnyHost) {
            config.bundledPlugins.enableCors { cors -> cors.addRule { it.anyHost() } }
        }

        rawConfig?.invoke(config)

        config.routes.apply {
            authHandler?.let { beforeMatched(it) }

            exception(Exception::class.java) { e, ctx ->
                val handler = exceptionHandler
                if (handler != null) {
                    handler(e, ctx)
                } else {
                    e.printStackTrace()
                    log.error("An unhandled exception occurred", e)
                    ctx.status(500)
                }
            }

            val error500 = errorHandlers[500]
            if (error500 != null) {
                error(500, error500)
            } else {
                error(500) { ctx ->
                    ctx.html("Sorry, an error occurred!")
                    ctx.status(500)
                }
            }

            val error404 = errorHandlers[404]
            if (error404 != null) {
                error(404, error404)
            } else {
                error(404) { ctx ->
                    ctx.result("No route matched for ${ctx.method()} ${ctx.path()}")
                }
            }

            errorHandlers.filterKeys { it != 500 && it != 404 }.forEach { (code, handler) -> error(code, handler) }

            if (accessLogEnabled) {
                after { ctx ->
                    accessLogger.info("${ctx.status()} ${ctx.path()} ${ctx.method()} ${ctx.queryString()}")
                }
            }

            install(*apis.toTypedArray())
        }

        log.info("Registered {} API resource(s)", apis.size)
        printRegisteredEndpoints()
    }
}

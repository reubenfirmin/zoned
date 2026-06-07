# Zoned Server API + Javalin 7 Migration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace zoned's post-creation Javalin route wiring with a zoned-owned `Zoned` server type that registers everything in Javalin 7's `config.routes` block, then migrate `interview` and `recipe` onto it.

**Architecture:** A new `Zoned` type runs a builder lambda to collect a spec (auth handler, APIs, static files, overridable middleware), then calls `Javalin.create { config -> … }` and pushes all registration into `config.routes`. Consumers never see the `Javalin` instance. The existing `Api`/annotation/`Response` model and reflection-based route discovery are reused — only the registration *receiver* changes from `Javalin` to `io.javalin.router.JavalinDefaultRoutingApi`.

**Tech Stack:** Kotlin 2.4.0 (multiplatform, JVM target), Javalin 7.2.2, Guice DI (`zoned.framework.di`), OkHttp 5 (tests), JUnit/kotlin-test, Gradle 9.5.1.

**Pre-applied in working tree (verify, do not redo):**
- JS: `js.core.asString` → `js.string.asString` in `SortableEnhancementImpl.kt`, `SelectableTableEnhancementImpl.kt`.
- Gradle plugin: `@DisableCachingByDefault` on 7 task types; `@PathSensitive(RELATIVE)` on `BuildStyleTask` inputs.
- Dependency versions bumped in `Versions.kt` + `build.gradle.kts` + `settings.gradle.kts` + wrapper.

**Context the implementer needs:**
- `config.routes` is `io.javalin.config.RoutesConfig`, which `implements io.javalin.router.JavalinDefaultRoutingApi`. That interface exposes `addHttpHandler(HandlerType, String, Handler, vararg RouteRole)`, `beforeMatched(Handler)`, `after(Handler)`, `exception(Class<E>, ExceptionHandler<E>)`, `error(int, Handler)`.
- `zoned.framework.auth.Role : io.javalin.security.RouteRole` (values `ANON, USER, ADMIN`). `Auth.handle` throws `UnauthorizedResponse` unless `ctx.routeRoles().any { userRole.isOrSuperiorTo(it) }` — so a route reachable by anon must declare `Role.ANON`.
- DI: `zoned.framework.di.set(block: KModule.() -> Unit)` registers bindings (eager); `inline fun <reified T> get(): T` resolves.
- `JavalinConfig` v7 still has `config.staticFiles.add { … }` and `config.bundledPlugins.enableCors { … }` (unchanged from v6).

---

## File Structure

**zoned (create):**
- `src/jvmMain/kotlin/zoned/framework/api/Zoned.kt` — the `Zoned` server type + companion `create`.
- `src/jvmMain/kotlin/zoned/framework/api/ZonedSpec.kt` — the builder/spec (`ZonedSpec`, `StaticFileEntry`).
- `src/jvmTest/kotlin/zoned/framework/api/ZonedServerTest.kt` — integration test.

**zoned (modify):**
- `src/jvmMain/kotlin/zoned/framework/api/RoutingExtensions.kt` — retarget `install`/`apiHandle` receiver `Javalin` → `JavalinDefaultRoutingApi`; rename to internal.
- `src/jvmMain/kotlin/zoned/framework/api/ContextExtensions.kt` — `handlerType()` → `method()` (3 sites).
- `build.gradle.kts` — add `okhttp` to `jvmTest` deps.

**interview (modify):** `src/jvmMain/kotlin/rcp/Main.kt`, `src/jvmMain/kotlin/rcp/ui/App.kt`.

**recipe (modify):** `src/jvmMain/kotlin/rcp/Main.kt`, `src/jvmMain/kotlin/rcp/ui/App.kt`, `src/jvmTest/kotlin/rcp/api/APITestBase.kt`.

---

## Task 1: Retarget route registration to the v7 routing API

**Files:**
- Modify: `src/jvmMain/kotlin/zoned/framework/api/RoutingExtensions.kt`
- Modify: `src/jvmMain/kotlin/zoned/framework/api/ContextExtensions.kt`

- [ ] **Step 1: Change the `ContextExtensions` handler-type calls**

In `ContextExtensions.kt`, Javalin 7 removed `Context.handlerType()`. Replace the three `handlerType()` calls in `fun Context.redirect(route: Route)` with `method()` (both return `io.javalin.http.HandlerType`):

```kotlin
fun Context.redirect(route: Route): Response {
    val currentMethod = when(method()) {
        HandlerType.PUT -> Method.PUT
        HandlerType.POST -> Method.POST
        HandlerType.GET -> Method.GET
        HandlerType.DELETE -> Method.DELETE
        HandlerType.PATCH -> Method.PATCH
        else -> throw Exception("No support for ${method()}")
    }
    if (currentMethod != route.method) {
        if (route.method == Method.GET) {
            location(route.url())
            html("ok")
        } else {
            throw Exception("Cannot redirect from a ${method()} to a ${route.method}")
        }
    } else {
        redirect(route.url())
    }
    return Response(Either.left(""))
}
```

- [ ] **Step 2: Retarget `install` and `apiHandle` in `RoutingExtensions.kt`**

Change the import and the two extension receivers from `io.javalin.Javalin` to `io.javalin.router.JavalinDefaultRoutingApi`, and make them `internal` (registration is now driven by `Zoned`, not consumers). The bodies are unchanged except the receiver type.

Replace the import:
```kotlin
import io.javalin.router.JavalinDefaultRoutingApi
```
(Remove `import io.javalin.Javalin` if no longer referenced elsewhere in the file — keep it only if other declarations use it.)

Change the signature on line ~31:
```kotlin
internal fun JavalinDefaultRoutingApi.install(vararg resources: Api) {
```
…body unchanged (it calls `apiHandle(routeAnnotation) { … }`)…

Change the signature on line ~140:
```kotlin
internal fun JavalinDefaultRoutingApi.apiHandle(authedRoute: AuthedRoute, eval: (Context) -> Response): BaseRoute {
    return with (authedRoute.route) {
        addHttpHandler(method.toJavalin(), path, { ctx ->
            // …unchanged body: eval(ctx), unwrap, ctx.target(...), ctx.html(...), ctx.json(...) …
        }, *authedRoute.roles)
        this
    }
}
```

`addHttpHandler(HandlerType, String, Handler, vararg RouteRole)` is declared on `JavalinDefaultRoutingApi`, and `authedRoute.roles` is `Array<out Role>` where `Role : RouteRole`, so the spread `*authedRoute.roles` type-checks unchanged.

- [ ] **Step 3: Verify (deferred to Task 3 compile)**

These two files cannot compile in isolation (the module also needs `Zoned`). Verification happens in Task 3 when the whole `zoned` JVM target compiles. Do **not** attempt a standalone build here.

- [ ] **Step 4: Commit**

```bash
cd ../zoned
git add src/jvmMain/kotlin/zoned/framework/api/RoutingExtensions.kt src/jvmMain/kotlin/zoned/framework/api/ContextExtensions.kt
git commit -m "Retarget zoned route registration to Javalin 7 config.routes API"
```

---

## Task 2: Create the `Zoned` server type + builder

**Files:**
- Create: `src/jvmMain/kotlin/zoned/framework/api/ZonedSpec.kt`
- Create: `src/jvmMain/kotlin/zoned/framework/api/Zoned.kt`

- [ ] **Step 1: Create `ZonedSpec.kt`**

```kotlin
package zoned.framework.api

import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.staticfiles.Location
import org.slf4j.LoggerFactory
import zoned.framework.di.KModule
import zoned.framework.di.set

/** A static-files mount to register on the Javalin config. */
data class StaticFileEntry(
    val hostedPath: String,
    val directory: String,
    val location: Location
)

/**
 * Collects a server specification via the [Zoned.create] builder lambda, then applies it to a
 * Javalin 7 [JavalinConfig] (all route/handler registration goes into `config.routes`).
 *
 * `bindings { }` is applied eagerly so that `apis(get<...>())` resolves against a populated injector.
 */
class ZonedSpec internal constructor() {

    private val log = LoggerFactory.getLogger("zoned.Zoned")
    private val accessLogger = LoggerFactory.getLogger("javalin.access")

    private var authHandler: Handler? = null
    private val apis = mutableListOf<Api>()
    private val staticFileEntries = mutableListOf<StaticFileEntry>()
    private var corsAnyHost = true
    private var accessLogEnabled = true
    private var exceptionHandler: ((Exception, Context) -> Unit)? = null
    private val errorHandlers = mutableMapOf<Int, Handler>()
    private var notFoundHandler: Handler? = null
    private var rawConfig: ((JavalinConfig) -> Unit)? = null

    /** Installed as `config.routes.beforeMatched(handler)`. */
    fun auth(handler: Handler) { authHandler = handler }

    /** Eagerly registers DI bindings (delegates to zoned.framework.di.set). */
    fun bindings(block: KModule.() -> Unit) { set(block) }

    /** APIs whose annotated routes will be registered. Resolve via get<T>() AFTER bindings{}. */
    fun apis(vararg resources: Api) { apis.addAll(resources) }

    fun staticFiles(hostedPath: String, directory: String, location: Location = Location.EXTERNAL) {
        staticFileEntries.add(StaticFileEntry(hostedPath, directory, location))
    }

    fun cors(anyHost: Boolean) { corsAnyHost = anyHost }
    fun accessLog(enabled: Boolean) { accessLogEnabled = enabled }
    fun onException(handler: (Exception, Context) -> Unit) { exceptionHandler = handler }
    fun onError(code: Int, handler: Handler) { errorHandlers[code] = handler }
    fun onNotFound(handler: Handler) { notFoundHandler = handler }

    /** Raw escape hatch for any JavalinConfig the helpers above don't wrap. */
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

            error(500, errorHandlers[500] ?: Handler { ctx ->
                ctx.html("Sorry, an error occurred!")
                ctx.status(500)
            })

            error(404, notFoundHandler ?: Handler { ctx ->
                ctx.result("No route matched for ${ctx.method()} ${ctx.path()}")
            })

            errorHandlers.filterKeys { it != 500 }.forEach { (code, handler) -> error(code, handler) }

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
```

- [ ] **Step 2: Create `Zoned.kt`**

```kotlin
package zoned.framework.api

import io.javalin.Javalin

/**
 * zoned-owned web server. Hides the underlying Javalin instance: callers configure routes,
 * auth, and middleware via [create]'s builder, then [start] it.
 */
class Zoned private constructor(private val javalin: Javalin) {

    fun start(port: Int): Zoned {
        javalin.start(port)
        return this
    }

    fun stop(): Zoned {
        javalin.stop()
        return this
    }

    companion object {
        fun create(block: ZonedSpec.() -> Unit): Zoned {
            val spec = ZonedSpec().apply(block)
            val javalin = Javalin.create { config -> spec.applyTo(config) }
            return Zoned(javalin)
        }
    }
}
```

- [ ] **Step 3: Verify (deferred to Task 3 compile)**

Cannot run in isolation. Compilation is verified in Task 3.

- [ ] **Step 4: Commit**

```bash
cd ../zoned
git add src/jvmMain/kotlin/zoned/framework/api/Zoned.kt src/jvmMain/kotlin/zoned/framework/api/ZonedSpec.kt
git commit -m "Add Zoned server type owning Javalin 7 creation + middleware"
```

---

## Task 3: Integration test, build, and publish zoned

**Files:**
- Modify: `build.gradle.kts` (add okhttp to jvmTest)
- Create: `src/jvmTest/kotlin/zoned/framework/api/ZonedServerTest.kt`

- [ ] **Step 1: Add okhttp to jvmTest deps**

In `build.gradle.kts`, in the `val jvmTest by getting { dependencies { … } }` block, add:
```kotlin
                implementation("com.squareup.okhttp3:okhttp:5.3.2")
```
(okhttp is already an `api` dep of jvmMain, but declaring it for jvmTest makes the test's intent explicit.)

- [ ] **Step 2: Write the failing integration test**

```kotlin
package zoned.framework.api

import io.javalin.http.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import zoned.framework.auth.Auth
import zoned.framework.auth.JWTAuthentication
import zoned.framework.auth.Role
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val PORT = 11091
private const val BASE = "http://localhost:$PORT"

class PublicApi : Api {
    override val basePath = "/test"
    override val baseRoles = emptyList<Role>()

    @GET("/hello", Role.ANON)
    fun hello(ctx: Context): Response = response { "hi" }

    @GET("/secret", Role.USER)
    fun secret(ctx: Context): Response = response { "secret" }
}

class ZonedServerTest {

    private val app: Zoned = Zoned.create {
        auth(Auth(JWTAuthentication("test-secret")))
        bindings { /* no bindings needed; PublicApi constructed directly */ }
        apis(PublicApi())
        accessLog(false)
    }

    private val client = OkHttpClient()

    @BeforeTest fun start() { app.start(PORT) }
    @AfterTest fun stop() { app.stop() }

    @Test
    fun `anon-allowed route returns 200`() {
        client.newCall(Request.Builder().url("$BASE/test/hello").get().build()).execute().use { resp ->
            assertEquals(200, resp.code)
            assertEquals("hi", resp.body!!.string())
        }
    }

    @Test
    fun `protected route returns 401 for anon`() {
        client.newCall(Request.Builder().url("$BASE/test/secret").get().build()).execute().use { resp ->
            assertEquals(401, resp.code)
        }
    }
}
```

- [ ] **Step 3: Run the test — expect it to drive compilation/behavior**

Run:
```bash
cd ../zoned && ./gradlew jvmTest --tests "zoned.framework.api.ZonedServerTest" --console=plain
```
Expected on first run if anything is wrong: compile errors or assertion failures. Iterate on Tasks 1–2 until both tests PASS. Common gotchas to check if it fails:
- `response { }` is `zoned.framework.api.response(...)` (returns HTML `Response`). Already imported via same package.
- If `error(500, Handler { … })` is ambiguous, use the lambda form `error(500) { ctx -> … }` and branch on the override beforehand.
- Confirm `config.routes` accessor name compiles; if not, it is `config.router` — adjust `ZonedSpec.applyTo` accordingly (RoutesConfig is the `routes` field per the v7 jar).

- [ ] **Step 4: Full build + publish**

Run:
```bash
cd ../zoned && ./gradlew build publishToMavenLocal --console=plain
```
Expected: `BUILD SUCCESSFUL`. The freshly built `~/.m2/repository/io/4rc/zoned-js/1.0-SNAPSHOT/zoned-js-1.0-SNAPSHOT.klib` and `zoned-jvm-1.0-SNAPSHOT.jar` timestamps update to now. If `kotlinStoreYarnLock` fails, run `./gradlew kotlinUpgradeYarnLock` once, then re-run.

- [ ] **Step 5: Commit**

```bash
cd ../zoned
git add build.gradle.kts src/jvmTest/kotlin/zoned/framework/api/ZonedServerTest.kt kotlin-js-store/yarn.lock
git commit -m "Add Zoned server integration test; publish updated zoned"
```

---

## Task 4: Migrate `interview` onto `Zoned`

**Files:**
- Modify: `/expanse/code/startup/interview/src/jvmMain/kotlin/rcp/ui/App.kt`
- Modify: `/expanse/code/startup/interview/src/jvmMain/kotlin/rcp/Main.kt`

- [ ] **Step 1: Rewrite `App.kt` as a `Zoned` factory**

```kotlin
package rcp.ui

import io.javalin.config.JavalinConfig
import io.javalin.http.staticfiles.Location
import rcp.InterviewConfig
import rcp.ui.admin.AdminApi
import rcp.ui.dashboard.DashboardApi
import rcp.ui.login.ForgotPasswordApi
import rcp.ui.login.InterviewMagicLinkProvider
import rcp.ui.login.LoginApi
import rcp.ui.registration.RegistrationApi
import zoned.framework.api.Zoned
import zoned.framework.auth.Auth
import zoned.framework.auth.JWTAuthentication
import zoned.framework.auth.magiclink.MagicLinkAuth
import zoned.framework.auth.magiclink.MagicLinkProvider
import zoned.framework.di.get
import javax.sql.DataSource

class App(private val dataSource: DataSource, private val appConfig: InterviewConfig) {

    private val jwt = JWTAuthentication(appConfig.jwtSecret)

    fun build(): Zoned = Zoned.create {
        auth(Auth(jwt))

        bindings {
            bind<JWTAuthentication>().toInstance(jwt)
            bind<DataSource>().toInstance(dataSource)
            bind<InterviewConfig>().toInstance(appConfig)
            bind<String>(Bindings.POSTMARK_SECRET).to(appConfig.postmarkSecret)
            bind<String>(Bindings.BASE_URL).to(appConfig.baseUrl)
            bind<String>(Bindings.ADMINS).to(appConfig.admins)
            bind<String>(Bindings.WHEREBY_SECRET).to(appConfig.wherebySecret)
            bind<MagicLinkProvider>().to<InterviewMagicLinkProvider>()
        }

        apis(
            get<MagicLinkAuth>(),
            get<RegistrationApi>(),
            get<DashboardApi>(),
            get<ForgotPasswordApi>(),
            get<LoginApi>(),
            get<AdminApi>()
        )

        staticFiles("/static", System.getProperty("user.dir") + "/dist", Location.EXTERNAL)

        javalin(App::config)
    }

    companion object {
        fun config(config: JavalinConfig) {
            // reserved for interview-specific raw Javalin config; CORS is a Zoned default.
        }
    }
}

object Bindings {
    const val POSTMARK_SECRET = "postmark_secret"
    const val WHEREBY_SECRET = "whereby_secret"
    const val BASE_URL = "baseUrl"
    const val ADMINS = "admins"
}
```

Notes: the default 404 in zoned already produces `"No route matched for {method} {path}"`, matching the old `handleUnmatchedRoute`, so no `onNotFound` override is needed. CORS anyHost is a zoned default. If interview needs the old explicit CORS, it is already covered.

- [ ] **Step 2: Rewrite `Main.kt`**

```kotlin
package rcp

import rcp.ui.App
import zoned.framework.config.Configurator
import zoned.framework.config.DataSourceProducer.Companion.provideSqlLitePooledDataSource
import kotlin.system.exitProcess

fun main() {
    try {
        val config = Configurator.load<InterviewConfig>()
        val dataSource = provideSqlLitePooledDataSource(config)
        println("Starting: " + System.getProperty("java.vm.name") + " " +
            System.getProperty("java.vm.version") + " " + System.getProperty("java.vm.vendor"))
        Migrate().migrate(config).migrate()
        App(dataSource, config).build().start(9000)
    } catch (e: Exception) {
        e.printStackTrace()
        exitProcess(1)
    }
}
```

- [ ] **Step 3: Build interview**

```bash
cd /expanse/code/startup/interview && ./gradlew build --console=plain
```
Expected: `BUILD SUCCESSFUL`. If `get<MagicLinkAuth>()` or other resolution fails at compile, it is a runtime DI call — only a compile check is expected here; resolution is exercised at startup.

- [ ] **Step 4: Commit**

```bash
cd /expanse/code/startup/interview
git add src/jvmMain/kotlin/rcp/ui/App.kt src/jvmMain/kotlin/rcp/Main.kt
git commit -m "Migrate interview to Zoned server API (Javalin 7)"
```

---

## Task 5: Migrate `recipe` onto `Zoned` (and its test harness)

**Files:**
- Modify: `/expanse/code/startup/recipe/src/jvmMain/kotlin/rcp/ui/App.kt`
- Modify: `/expanse/code/startup/recipe/src/jvmMain/kotlin/rcp/Main.kt`
- Modify: `/expanse/code/startup/recipe/src/jvmTest/kotlin/rcp/api/APITestBase.kt`

- [ ] **Step 1: Rewrite `App.kt` as a `Zoned` factory**

```kotlin
package rcp.ui

import io.javalin.config.JavalinConfig
import io.javalin.http.staticfiles.Location
import org.slf4j.LoggerFactory
import rcp.RecipeConfig
import rcp.service.MenuMarginUpdater
import rcp.ui.billing.BillingApi
import rcp.ui.dashboard.DashboardApi
import rcp.ui.feedback.FeedbackApi
import rcp.ui.ingredients.IngredientApi
import rcp.ui.login.ForgotPasswordApi
import rcp.ui.login.LoginApi
import rcp.ui.menus.MenuApi
import rcp.ui.recipes.RecipeApi
import rcp.ui.registration.RegistrationApi
import rcp.ui.settings.SettingsApi
import rcp.ui.suppliers.SuppliersApi
import rcp.ui.team.TeamApi
import zoned.framework.api.Zoned
import zoned.framework.auth.Auth
import zoned.framework.auth.JWTAuthentication
import zoned.framework.config.Config
import zoned.framework.di.get
import java.util.concurrent.Executors
import javax.sql.DataSource

class App(private val dataSource: DataSource, private val appConfig: RecipeConfig) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val jwt = JWTAuthentication(appConfig.jwtSecret)

    fun build(): Zoned = Zoned.create {
        auth(Auth(jwt))

        bindings {
            bind<RecipeConfig>().toInstance(appConfig)
            bind<JWTAuthentication>().toInstance(jwt)
            bind<DataSource>().toInstance(dataSource)
            bind<Config>().toInstance(appConfig)
            bind<String>(Bindings.POSTMARK_SECRET).to(appConfig.postmarkSecret)
            bind<String>(Bindings.BASE_URL).to(appConfig.baseUrl)
            bind<String>(Bindings.ADMINS).to(appConfig.admins)
        }

        apis(
            get<SettingsApi>(),
            get<RegistrationApi>(),
            get<DashboardApi>(),
            get<IngredientApi>(),
            get<SuppliersApi>(),
            get<MenuApi>(),
            get<RecipeApi>(),
            get<ForgotPasswordApi>(),
            get<LoginApi>(),
            get<BillingApi>(),
            get<TeamApi>(),
            get<FeedbackApi>()
        )

        staticFiles("/static", System.getProperty("user.dir") + "/dist", Location.EXTERNAL)

        javalin(App::config)
    }

    /** Starts the background margin-updater worker. Call after start(). */
    fun startWorkers() {
        Executors.newFixedThreadPool(1).submit {
            while (true) {
                try {
                    get<MenuMarginUpdater>().process()
                } catch (e: Exception) {
                    log.error("Error running margin updater", e)
                    Thread.sleep(1000000)
                }
            }
        }
    }

    companion object {
        fun config(config: JavalinConfig) {
            // reserved for recipe-specific raw Javalin config; CORS is a Zoned default.
        }
    }
}

object Bindings {
    const val POSTMARK_SECRET = "postmark_secret"
    const val BASE_URL = "base_url"
    const val ADMINS = "admins"
}
```

- [ ] **Step 2: Rewrite `Main.kt`**

```kotlin
package rcp

import rcp.ui.App
import zoned.framework.config.Configurator
import zoned.framework.config.DataSourceProducer.Companion.providePostgresDataSource
import kotlin.system.exitProcess

fun main() {
    try {
        val config = Configurator.load<RecipeConfig>()
        val dataSource = providePostgresDataSource(config)
        println("Starting: " + System.getProperty("java.vm.name") + " " +
            System.getProperty("java.vm.version") + " " + System.getProperty("java.vm.vendor"))
        val app = App(dataSource, config)
        app.build().start(9000)
        app.startWorkers()
    } catch (e: Exception) {
        e.printStackTrace()
        exitProcess(1)
    }
}
```

- [ ] **Step 3: Update `APITestBase.kt` to the new factory**

Replace the `Javalin`-based lifecycle (lines defining `server` + `init` + `@BeforeTest`/`@AfterTest`) with the `Zoned` factory. Change imports: remove `import io.javalin.Javalin`; add `import zoned.framework.api.Zoned`. Replace the fields/lifecycle:

```kotlin
    private val app: Zoned = App(testDataSource, testConfig).build()
    val objectMapper = ObjectMapper().registerKotlinModule()

    init {
        objectMapper.findAndRegisterModules()
    }

    @BeforeTest
    fun `start javalin`() {
        app.start(TEST_PORT)
    }

    @AfterTest
    fun `stop javalin`() {
        app.stop()
    }
```

(The rest of `APITestBase` — request builders, `jwt`/`anon`, `unpack` — is unchanged. `App` no longer constructs workers in `build()`, so tests do not spawn the margin worker. Good.)

- [ ] **Step 4: Build recipe and run the API test suite**

```bash
cd /expanse/code/startup/recipe && ./gradlew build --console=plain
```
Expected: `BUILD SUCCESSFUL`, including `jvmTest`. The `APITestBase`-derived tests start a real `Zoned` server on `TEST_PORT`, issue JWT-authenticated and anon requests, and assert responses — this is the end-to-end validation that route registration + auth + responses work under Javalin 7.

If `jvmTest` requires a database (DBTestBase), and none is available in the environment, run at least the compile + a non-DB subset:
```bash
cd /expanse/code/startup/recipe && ./gradlew compileTestKotlinJvm --console=plain
```
and report that DB-backed tests need a live database.

- [ ] **Step 5: Commit**

```bash
cd /expanse/code/startup/recipe
git add src/jvmMain/kotlin/rcp/ui/App.kt src/jvmMain/kotlin/rcp/Main.kt src/jvmTest/kotlin/rcp/api/APITestBase.kt
git commit -m "Migrate recipe to Zoned server API (Javalin 7)"
```

---

## Task 6: Verify thedozone2 builds against the republished zoned

**Files:** none (verification only).

- [ ] **Step 1: Build thedozone2**

```bash
cd /expanse/code/startup/thedozone2 && ./gradlew build --console=plain
```
Expected: `BUILD SUCCESSFUL`. thedozone2 is JS-only; it consumes `zoned-js` (already republished in Task 3). If `kotlinStoreYarnLock` fails due to the nomnoml bump, run `./gradlew kotlinUpgradeYarnLock` once and re-run.

- [ ] **Step 2: Commit thedozone2 dependency bumps**

```bash
cd /expanse/code/startup/thedozone2
git add build.gradle.kts settings.gradle.kts gradle/wrapper/gradle-wrapper.properties kotlin-js-store/yarn.lock
git commit -m "Update Kotlin 2.4.0, Gradle 9.5.1, node-gradle 7.1.0, foojay 1.0.0, nomnoml 1.7.0"
```

---

## Self-Review

- **Spec coverage:** `Zoned.create{}.start()` (Task 2), zoned-owned overridable defaults exception/500/404/access-log/CORS (Task 2 `ZonedSpec.applyTo`), removal of public `Javalin.install` (Task 1, made `internal`), `handlerType()`→`method()` (Task 1), `js.string.asString` + plugin annotations (pre-applied, noted in header), both apps migrated + recipe test harness (Tasks 4–5), zoned build/publish + thedozone2 verify (Tasks 3, 6). `stop()` added to `Zoned` for the test harness (Task 2/3) — an addition beyond the spec's `start()`, required by `APITestBase`.
- **Placeholders:** none — every code step contains full code.
- **Type consistency:** `Zoned.create(block) → Zoned`; `Zoned.start(Int)/stop() → Zoned`; `ZonedSpec` methods `auth/bindings/apis/staticFiles/cors/accessLog/onException/onError/onNotFound/javalin`; `install`/`apiHandle` receiver `JavalinDefaultRoutingApi`. `App(dataSource, config).build(): Zoned` used identically in interview/recipe Main + recipe APITestBase.

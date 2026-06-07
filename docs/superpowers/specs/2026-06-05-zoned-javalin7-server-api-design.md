# Design: `Zoned` server API + Javalin 7 migration

**Date:** 2026-06-05
**Status:** Approved (pending written-spec review)
**Repos affected:** `zoned` (core), `interview`, `recipe` (consumers). `thedozone2` is JS-only and unaffected at the source level.

## Background

As part of bumping all external dependencies to latest stable, `zoned` moved to
Javalin 7. Javalin 7 removed post-creation route registration: you can no longer
call `app.get(...)` / `app.addHttpHandler(...)` / `app.before(...)` / `app.error(...)`
on a created `Javalin` instance. All routes and handlers must be registered inside
the `Javalin.create { config -> config.routes.* }` block, before `start()`.

`zoned`'s current routing framework (`zoned.framework.api.RoutingExtensions`) is built
around the old model: `fun Javalin.install(vararg resources: Api)` reflects over each
`Api`'s annotated methods and calls `Javalin.apiHandle(...)` → `addHttpHandler(...)` on a
*running* instance. The consumer apps (`interview`, `recipe`) follow the same pattern in
`Main.kt` / `App.kt`: `Javalin.create(App::config)`, then on the instance call
`beforeMatched(Auth)`, `exception(...)`, `error(500/404)`, `after(accessLog)`,
`install(apis...)`, then `start(port)`.

This design replaces that with a single zoned-owned server type that hides Javalin
entirely and registers everything in the v7 `config.routes` block.

## Goals

- Prefer zoned's API over Javalin: consumer apps never touch the `Javalin` instance.
- Absorb the Javalin 6→7 routing change entirely within `zoned`.
- Collapse the boilerplate repeated across apps (auth, exception/error, access log)
  into zoned defaults, each overridable.
- Keep the existing `Api` / annotation (`@GET`/`@POST`/…) / `Response` programming
  model unchanged — only the server wiring changes.

## Non-goals

- No change to the `Api` interface, route annotations, `Response`/`AuxResponse`, the
  HTMX response flow, or the DI (`zoned.framework.di`) API.
- No new auth policy. Auth remains app-supplied (app constructs and passes the handler).
- No websocket/SSE surface changes.

## Consumer API

New type `Zoned` in `zoned.framework.api` (server entry point). Builder collects a
spec, then internally calls `Javalin.create { … }` and returns a started-or-startable
wrapper.

```kotlin
val app = Zoned.create(appConfig) {
    auth(Auth(JWTAuthentication(appConfig.jwtSecret)))   // -> config.routes.beforeMatched(handler)

    bindings {                                            // -> zoned.framework.di set{}, applied EAGERLY
        bind<DataSource>().toInstance(dataSource)
        bind<JWTAuthentication>().toInstance(jwt)
        bind<MagicLinkProvider>().to<InterviewMagicLinkProvider>()
    }

    apis(                                                 // resolved AFTER bindings; routes registered in config.routes
        get<LoginApi>(),
        get<RegistrationApi>(),
        get<DashboardApi>(),
        /* … */
    )

    staticFiles("/static", "$cwd/dist")                  // -> config.staticFiles.add, Location.EXTERNAL default

    // Optional overrides (each has a zoned default):
    onNotFound { ctx -> ctx.result("No route matched for ${ctx.method()} ${ctx.path()}") }
    // onException { e, ctx -> … }
    // onError(500) { ctx -> … }
    // accessLog(enabled = true)
    // cors(anyHost = true)
    // javalin { c -> /* raw JavalinConfig escape hatch */ }
}

app.start(9000)
// recipe: launch its MenuMarginUpdater worker thread here, after start() returns.
```

### Defaults (overridable)

| Concern        | Default behavior                                                  | Override            |
|----------------|-------------------------------------------------------------------|---------------------|
| Exception      | log stacktrace + `ctx.status(500)`                                | `onException { }`   |
| Error 500      | `ctx.html("Sorry, an error occurred!")`                           | `onError(500) { }`  |
| Error 404      | simple `"No route matched …"` text                                | `onNotFound { }`    |
| Access log     | `after { log "${status} ${path} ${method} ${queryString}" }`      | `accessLog(false)`  |
| CORS           | `bundledPlugins.enableCors { anyHost() }`                         | `cors(anyHost=…)` / `javalin{}` |
| Registered eps | logged at startup (replaces `printRegisteredEndpoints()`)         | n/a                 |

## Internal design (zoned)

### `Zoned` + `ZonedSpec` (builder) — new

- `Zoned.create(appConfig: Any? = null, block: ZonedSpec.() -> Unit): Zoned`
- `ZonedSpec` collects: the auth `Handler`, the list of `Api` instances, static-files
  entries, CORS toggle, and the overridable hooks. `bindings { }` delegates to the
  existing DI `set { }` and is applied **eagerly** during the builder lambda, so that
  `apis(get<…>())` (which resolves DI at call time) sees a populated container.
- `Zoned.create` then calls `Javalin.create { config -> … }`:
  - apply `staticFiles` / CORS / raw `javalin{}` onto `config`;
  - inside `config.routes`:
    - `beforeMatched(authHandler)` (if auth supplied),
    - `exception(Exception::class)`, `error(500)`, `error(404)`, `after(accessLog)` (defaults or overrides),
    - for each `Api`, run the reflection-based route discovery and register each route
      via `addHttpHandler(method.toJavalin(), path, handler, *roles)`.
  - store the resulting `Javalin` privately.
- `Zoned.start(port: Int): Zoned` → `javalin.start(port)`; returns `this`.

### `RoutingExtensions` — retargeted

- The route-discovery logic currently in `Javalin.install` / `Javalin.apiHandle` is
  retargeted from a `Javalin` receiver to the `config.routes` routing API
  (`io.javalin.router.JavalinDefaultRoutingApi`), which exposes
  `addHttpHandler(HandlerType, String, Handler, vararg RouteRole)`.
- Public `fun Javalin.install(...)` extension is **removed**; registration is internal,
  driven by `Zoned`.
- The `formContextualRoutes` / `contextualRoutes` / `nonContextualRoutes` registries,
  `route(...)` lookup, `Response`/`AuxResponse`, and `MethodIdentifier` are unchanged.

### `ContextExtensions` — mechanical

- Javalin 7 removed `Context.handlerType()`; replace with `Context.method()` (returns
  `HandlerType`). 3 call sites in `ContextExtensions.kt` (`redirect`).

### JS side — already applied

- kotlin-wrappers BOM bump moved `asString` from `js.core` to `js.string`. Imports in
  `SortableEnhancementImpl.kt` and `SelectableTableEnhancementImpl.kt` updated. (Done.)

### Gradle plugin — already applied

- Gradle 9.5.1 stricter plugin validation: 7 custom task types annotated
  `@DisableCachingByDefault`; `BuildStyleTask` input-file properties given
  `@PathSensitive(PathSensitivity.RELATIVE)`. (Done.)

## App migration

### `interview` and `recipe`

- `App` becomes a small factory that builds and returns the configured `Zoned` instance:
  `class App(dataSource, config) { fun build(): Zoned = Zoned.create(config){ … } }` (or a
  top-level `fun buildApp(...): Zoned`). `Main.kt` then becomes:
  `App(dataSource, config).build().start(9000)` followed by any post-start work. The
  `Javalin.create(App::config)` / `App(server, …)` / `server.start(9000)` sequence is removed.
- `App.kt`: the `init` block's `beforeMatched`/`exception`/`error(500)`/`after`/`install`
  collapse into the `Zoned.create` builder. App-specific remainders:
  - DI `set { }` → `bindings { }`.
  - `App.config`'s CORS + static files → `staticFiles(...)` + default CORS (or `javalin{}`).
  - interview's custom `handleUnmatchedRoute` → `onNotFound { }`.
  - recipe's `MenuMarginUpdater` worker → launched after `start()`.

## Validation

1. `zoned`: `./gradlew build publishToMavenLocal` — JVM **and** JS compile + publish.
2. `interview`: `./gradlew build` (JVM compiles against the new API).
3. `recipe`: `./gradlew build` and run `jvmTest` `APITestBase` suite — end-to-end check
   that route registration + auth + responses still work under Javalin 7.

## Risks / open questions

- Exact accessor name for the routing block (`config.routes`) and the roles-aware
  `addHttpHandler` overload are confirmed from the Javalin 7 jar; confirm at compile time.
- `ctx.routeRoles()` (used by `Auth`) must still be populated when roles are passed via
  `addHttpHandler(..., *roles)` — verify via recipe's auth tests.
- `bindings{}`-before-`apis()` ordering depends on eager DI application inside the builder
  lambda; the builder must apply bindings synchronously, not defer them.

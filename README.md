# Zoned

**Typesafe, full-stack web apps in Kotlin — where the server stays in control.**

Zoned is a [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) framework for
building server-rendered web applications with light, typesafe client-side enhancements. You write
the server in Kotlin, you write the browser code in Kotlin, and you share models, serialization, and
contracts between them. The server renders complete HTML; the client only adds interactivity.

It's a hypermedia-first approach (in the spirit of HTMX / Hotwire) but with end-to-end type safety:
routes are typed callables, forms deserialize into validated Kotlin objects, and client behaviors
("enhancements") have serializable configs that are generated into a typed server-side DSL.

---

## Project goals

- **The frontend is as dumb as possible.** Render as much as possible into the markup on the
  server. The client never fetches data or decides what to display — it adds event handlers,
  animations, and polish to HTML the server already produced.
- **Type safety end to end.** One language (Kotlin) across the server, the browser, and the shared
  domain. Routes, forms, HTML (via [kotlinx.html](https://github.com/Kotlin/kotlinx.html)), CSS (via
  kotlinx.css), and enhancement configs are all typed and checked at compile time.
- **Hypermedia over JSON APIs.** Interactions exchange HTML fragments (HTMX), not ad-hoc JSON
  endpoints. The server remains the single source of truth.
- **Batteries included, conventions over wiring.** A Gradle plugin handles the tedious parts:
  enhancement code generation, JS bundle configuration, database migration/codegen, Tailwind
  builds, and a hot-reload dev script.
- **Two project shapes from one framework.** Build a full-stack app (JVM server + JS client) or a
  frontend-only single-page app (JS only) with the same primitives.

### What's in the box

| Area | Built on |
|------|----------|
| HTTP server | [Javalin](https://javalin.io/) |
| HTML / CSS | kotlinx.html ([Zoned fork](#the-kotlinxhtml-fork)), kotlinx.css (typed DSLs) |
| Hypermedia | [HTMX](https://htmx.org/) |
| Database | [jOOQ](https://www.jooq.org/) (typesafe SQL) + [Flyway](https://flywaydb.org/) migrations, PostgreSQL & SQLite |
| Auth | JWT, magic-link login, bcrypt, role-based access |
| Email | Postmark |
| DI | kotlin-guice |
| Styling | Tailwind CSS |
| Client libs (wrapped) | Ace, Sortable, ApexCharts, Tribute, Leaflet, Flowbite, Prism, and more |

**Versions:** Kotlin `2.4.0` · Gradle `9.5.1` · JDK 21+.

---

## How it works

An **enhancement** is the unit of client-side interactivity. It has three parts:

1. **A common definition** (`commonMain`) — a `@ClientEnhancement` object plus a `@Serializable`
   config data class, shared by server and client.
2. **A generated server DSL** — the Gradle plugin scans your enhancements and generates typed
   builder functions, so on the server you write `tooltip({ text = "Hi" }) { span { +"Hover me" } }`.
   That renders a wrapper `<div data-enhancement="tooltip" data-enhancement-config='{"text":"Hi"}'>`.
3. **A client implementation** (`jsMain`) — a `@EnhancementImpl(...)` function that reads the config
   off the element and wires up the behavior. The plugin generates a registry that dispatches to it
   on page load (and on HTMX swaps).

See [`CLAUDE.md`](./CLAUDE.md) for the full architecture guide, the rendering model, and the rules
for building client-side UI.

---

## The kotlinx.html fork

Zoned renders all of its HTML — on both the server and the client — with
[kotlinx.html](https://github.com/Kotlin/kotlinx.html). Rather than the upstream release, it depends
on a **fork** that's required to build Zoned:

- **Repository:** <https://github.com/reubenfirmin/kotlinx-html-new>
- **Coordinates:** `org.jetbrains.kotlinx:kotlinx-html:0.12.0-web` — it keeps the upstream
  group/artifact and distinguishes itself with the `-web` version suffix, so it transparently
  replaces stock kotlinx.html on the classpath.
- **License:** Apache-2.0 (inherited from upstream).

### Why it exists

The client-side enhancement model leans on a richer DOM-building experience than upstream
kotlinx.html offers for Kotlin/JS. The fork adds:

- **Typed DSL event handlers.** DSL events carry their real DOM event type instead of a generic
  `Event` — `onClick { event /* MouseEvent */ -> }`, plus `KeyboardEvent`, `FocusEvent`, etc. This
  is what makes Zoned's "bind live listeners straight from the DSL" model typesafe. (On WasmJS the
  handler stays a generic `Event` due to lambda-casting limits.)
- **Real DOM APIs instead of `innerHTML` hacks.** Upstream JS interop paths that relied on
  `innerHTML` string manipulation are replaced with proper DOM calls — aligning with Zoned's
  forbidden-patterns rules and its `ElementTrackingConsumer` (the custom consumer behind `ref` /
  `onMount` lifecycle).
- **Migration to kotlin-wrappers `web.*` packages**, with `kotlin-js` and `kotlin-browser` exposed
  as API dependencies, built against **Kotlin 2.4.0** and **kotlin-wrappers 2026.6.2** to match the
  rest of the Zoned stack.

You don't depend on the fork directly in your app — Zoned pulls it in transitively. You only need to
have published it to Maven Local first (step 1 of [Getting started](#getting-started)).

---

## Getting started

Zoned is currently distributed as `1.0-SNAPSHOT` via your **local Maven repository** — it is not yet
published to a public repository. So the first steps for any app are to build and publish Zoned (and
its kotlinx.html fork) locally.

### 1. Publish the kotlinx.html fork to Maven Local

Zoned depends on a fork of kotlinx.html, published as `org.jetbrains.kotlinx:kotlinx-html:0.12.0-web`.
It isn't on Maven Central, so build it first (see [The kotlinx.html fork](#the-kotlinxhtml-fork)
below for what it changes and why):

```bash
git clone https://github.com/reubenfirmin/kotlinx-html-new
cd kotlinx-html-new
./gradlew publishToMavenLocal
```

### 2. Publish Zoned to Maven Local

```bash
git clone <this-repo> zoned
cd zoned
./gradlew publishToMavenLocal
```

This publishes both the library (per-target artifacts `io.4rc:zoned-jvm` and `io.4rc:zoned-js`) and
the Gradle plugin (`io.4rc.zoned.plugin`) to `~/.m2`. Re-run it whenever you change framework code so
dependent apps pick up the update.

### 3. Point your app at Maven Local

In your app's `settings.gradle.kts`, make sure `mavenLocal()` is available to plugins:

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

And in `build.gradle.kts` repositories:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}
```

From here, pick your project shape: **[frontend-only](#frontend-only-apps)** or
**[full-stack](#full-stack-apps)**.

---

## Frontend-only apps

A frontend-only app is a Kotlin/JS single-page application: no server, no database, no bundle served
by a backend. The Zoned Gradle plugin auto-detects the absence of a JVM target and configures the
dev workflow accordingly (it uses port **3000** and the `jsBrowserDevelopmentRun` server task).

### build.gradle.kts

Declare only a JS target, and depend on `io.4rc:zoned-js`:

```kotlin
plugins {
    kotlin("multiplatform") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
    id("io.4rc.zoned.plugin") version "1.0-SNAPSHOT"
}

kotlin {
    jvmToolchain(21)

    js(IR) {
        browser {
            runTask {
                mainOutputFileName.set("main.bundle.js")
                sourceMaps = true
            }
            webpackTask {
                mainOutputFileName.set("main.bundle.js")
            }
        }
        binaries.executable()
    }

    sourceSets["jsMain"].dependencies {
        implementation("io.4rc:zoned-js:1.0-SNAPSHOT")
        // any extra npm libs your app needs:
        // implementation(npm("nomnoml", "1.7.0"))
    }
}
```

> **No `jvm { }` block = frontend-only.** That single difference is what tells the plugin (and you)
> that there's no backend, no database, and no server-served bundle to configure.

### Project layout

```
src/jsMain/kotlin/<pkg>/   # your app code (entry point, views, models, controllers)
src/jsMain/resources/      # index.html, css
```

### Entry point and routing

Your `main()` sets up client-side routes with the typed `Routes` / `Router` DSL and starts the
router:

```kotlin
import zoned.framework.routing.RenderMode
import zoned.framework.routing.Router
import zoned.framework.routing.Routes

class App {
    object index : Routes<Index>(Index(model, eventBus), "/") {
        val home   = route { "/" to { index(model.currentCanvas()) } }
        val canvas = route(mode = RenderMode.PARTIAL) {
            "/{canvas...}" to { params -> /* ... */ }
        }
    }
}

fun main() {
    App()
    Router.start()
}
```

Views are built with the kotlinx.html DSL plus Zoned's DOM/interop helpers; styling combines
Tailwind class names with the typed `css {}` DSL.

### Run it

```bash
./watch.sh                          # generated hot-reload dev loop (tmux) → http://localhost:3000
# or, manually:
./gradlew jsBrowserDevelopmentRun   # dev server with hot reload
./gradlew jsBrowserProductionWebpack  # production bundle
```

---

## Full-stack apps

A full-stack app has a JVM server (Javalin) that renders HTML and serves a compiled JS **bundle**,
plus a Kotlin/JS client that hydrates that HTML with enhancements. Both targets live in one Gradle
module and share `commonMain`.

### build.gradle.kts

Declare **both** targets. Depend on `io.4rc:zoned-jvm` in `jvmMain` and `io.4rc:zoned-js` in
`jsMain`:

```kotlin
plugins {
    kotlin("multiplatform") version "2.4.0"
    id("com.gradleup.shadow") version "9.1.0"          // fat jar for deployment
    id("io.4rc.zoned.plugin") version "1.0-SNAPSHOT"
}

kotlin {
    jvm {
        testRuns.named("test") { executionTask.configure { useJUnitPlatform() } }
    }
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport { enabled.set(true) }
                // outputFileName is auto-configured by the zoned plugin
            }
            webpackTask { output.libraryTarget = "umd" }
        }
        binaries.executable()
    }

    sourceSets {
        val jvmMain by getting {
            dependencies { implementation("io.4rc:zoned-jvm:1.0-SNAPSHOT") }
        }
        val jsMain by getting {
            dependencies { implementation("io.4rc:zoned-js:1.0-SNAPSHOT") }
        }
    }
}
```

### Project layout

```
src/commonMain/kotlin/<pkg>/   # shared models, enhancement definitions
src/jvmMain/kotlin/<pkg>/      # Main.kt, App.kt, routes/APIs, db, server-side UI
src/jvmMain/resources/
    assets/                    # static assets (images, etc.) → copied to dist/
    db/migration/              # Flyway migrations (V1__*.sql)
src/jsMain/kotlin/<pkg>/       # MainBundle.kt (JS entry), enhancement impls, client libs
webpack.config.d/             # custom webpack config snippets (auto-merged)
dist/                         # runtime output: the bundle + static assets served at /static
```

### Bundle setup

This is the part that's easy to get wrong, so here's the whole picture. A **bundle** is the compiled
Kotlin/JS + CSS output that the server serves to the browser. Zoned automates most of it, but you
have to wire up four things in your app.

**What the Gradle plugin does for you automatically:**

- Derives the bundle name from your project name: `<projectName>.bundle.js`.
- Auto-configures the webpack `outputFileName` to that name (no manual `mainOutputFileName` needed
  for full-stack apps — that's why the snippet above omits it).
- Generates `BundleConfig.kt` (in `build/generated/kotlin/zoned/framework/ui/libs/`) exposing:
  ```kotlin
  object BundleConfig {
      const val BUNDLE_NAME = "<projectName>.bundle.js"
      const val BUNDLE_PATH = "/static/<projectName>.bundle.js"
  }
  ```
  The framework uses this to inject the correct `<script>` into every page's `<head>`. You don't
  hand-write the script tag.

**What you wire up in your app:**

**1. A JS entry point.** Export a `main()` (and an init object) from `jsMain` so webpack has an
entry. Initialize your client libraries / enhancement registry here:

```kotlin
// src/jsMain/kotlin/<pkg>/ui/MainBundle.kt
@JsExport
object MainBundle {
    init {
        // initFlowbite(); setupHTMX(...); addHelpers(); etc.
    }
}

@JsExport
fun main() { /* entry */ }
```

**2. Serve `/static` from `dist/` on the server.** In your `App.build()`:

```kotlin
import io.javalin.http.staticfiles.Location

staticFiles("/static", System.getProperty("user.dir") + "/dist", Location.EXTERNAL)
```

This matches `BundleConfig.BUNDLE_PATH` (`/static/<projectName>.bundle.js`). To serve the bundle
under `/static` from webpack's dev server too, add a one-liner in `webpack.config.d/`:

```js
// webpack.config.d/js-bundle-directory.js
config.output.publicPath = '/static'
```

**3. Copy the built bundle (and assets) into `dist/`.** The server reads from `dist/` at runtime, so
copy webpack's production output there and make `run` depend on it:

```kotlin
tasks.register<Copy>("copyBundle") {
    from("build/dist/js/productionExecutable")   // contains <projectName>.bundle.js
    into("dist")
    dependsOn("jsBrowserDistribution")
}
tasks.register<Copy>("copyResources") {
    from("src/jvmMain/resources/assets")
    into("dist")
}
tasks.register<JavaExec>("run") {
    dependsOn("copyBundle", "copyResources", "jvmMainClasses")
    mainClass.set("<pkg>.MainKt")
    val main = kotlin.jvm().compilations.getByName("main")
    classpath(main.output, main.runtimeDependencyFiles)
}
```

**4. Declare extra npm dependencies in `build.gradle.kts`** (not a separate `package.json`):

```kotlin
val jsMain by getting {
    dependencies {
        implementation("io.4rc:zoned-js:1.0-SNAPSHOT")
        implementation(npm("@whereby.com/browser-sdk", "3.10.10"))
    }
}
```

That's the full loop: **plugin names + configures + injects the bundle → you produce it, copy it to
`dist/`, and serve `dist/` at `/static`.**

### Server entry point

```kotlin
// src/jvmMain/kotlin/<pkg>/Main.kt
fun main() {
    val config = Configurator.load<MyConfig>()
    val dataSource = provideSqlLitePooledDataSource(config)
    Migrate().migrate(config).migrate()
    App(dataSource, config).build().start(9000)   // Javalin on :9000
}
```

The server is assembled with `Zoned.create { ... }` — registering auth, DI bindings, your APIs, and
static file serving:

```kotlin
// src/jvmMain/kotlin/<pkg>/ui/App.kt
fun build(): Zoned = Zoned.create {
    auth(Auth(jwt))
    bindings {
        bind<DataSource>().toInstance(dataSource)
        bind<MyConfig>().toInstance(appConfig)
        // ... other bindings
    }
    apis(
        get<MagicLinkAuth>(),
        get<DashboardApi>(),
        // ... your APIs
    )
    staticFiles("/static", System.getProperty("user.dir") + "/dist", Location.EXTERNAL)
}
```

### Database

```bash
./gradlew db-migrate    # run Flyway migrations (src/jvmMain/resources/db/migration)
./gradlew db-gen        # generate jOOQ models from the migrated schema (run after db-migrate)
```

Zoned supports PostgreSQL and SQLite; configuration is loaded via the `Configurator` (env / `.env`).

### Run it

```bash
./watch.sh    # generated: runs the JVM server, JS webpack, and Tailwind together with hot reload
```

Or the equivalent in separate terminals:

```bash
./gradlew run -t            # JVM server  → http://localhost:9000 (serves the bundle from dist/)
./gradlew build-style -t    # Tailwind CSS rebuild on change
./gradlew jsBrowserRun -t   # webpack dev server → http://localhost:8080 (proxies backend, hot JS)
```

- **:9000** — the real server; uses the bundle in `dist/` (rebuilt by `copyBundle`).
- **:8080** — webpack dev server for fast JS iteration; proxies API calls to the backend.

Build a deployable fat jar with `./gradlew deployable` (uses the Shadow plugin).

---

## The Gradle plugin

Applying `id("io.4rc.zoned.plugin")` gives you these tasks (and wires them into compilation where
appropriate):

| Task | Purpose |
|------|---------|
| `generate-enhancements` | Scan `@ClientEnhancement` / `@EnhancementImpl`, generate the server DSL + client registry |
| `generate-bundle-config` | Generate `BundleConfig.kt` (bundle name & path) |
| `scaffold-enhancement` | Create boilerplate for a new enhancement |
| `db-migrate` | Run Flyway migrations |
| `db-gen` / `model-generate` | Generate jOOQ models from the schema |
| `db-clean` | Clear the database |
| `build-style` | Compile Tailwind CSS |
| `generate-watch-script` | (Re)generate `watch.sh` for the dev loop |

Code generation is hooked into the build automatically — `compileKotlinJvm` depends on bundle config
+ enhancement generation, and `compileKotlinJs` depends on enhancement generation — so you rarely
invoke these by hand.

---

## Status

Zoned is `1.0-SNAPSHOT` and consumed via **Maven Local** (`publishToMavenLocal`); it is not yet on a
public Maven repository. APIs may change.

## License

[MIT](./LICENSE).

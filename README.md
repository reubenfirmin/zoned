# Zoned

Zoned is a Kotlin Multiplatform framework for web applications. A Javalin server renders
HTML with kotlinx.html; HTMX requests replace parts of the page. Kotlin/JS adds browser
behavior such as editors, tooltips, and drag-and-drop. Models and enhancement configs
can live in `commonMain` and be shared by both targets.

The framework includes form conversion and validation, JWT and magic-link auth,
jOOQ/Flyway support for PostgreSQL and SQLite, Tailwind builds, and Kotlin wrappers for
browser libraries. You can also use the JS target on its own.

Zoned is published locally as `1.0-SNAPSHOT`. APIs may change. This checkout uses
Kotlin 2.4.0, Gradle 9.5.1, and JDK 21.

## Setup

### Publish the framework

```bash
git clone https://github.com/reubenfirmin/zoned.git
cd zoned
```

On a fresh machine, bootstrap the Gradle plugin first. Temporarily comment out
`id("io.4rc.zoned.plugin") version "1.0-SNAPSHOT"` in the root `build.gradle.kts`, then run:

```bash
./gradlew :gradle-plugin:publishToMavenLocal
```

Restore that line, then publish the library and plugin:

```bash
./gradlew publishToMavenLocal
```

Repeat the last command after changing framework code so apps using Maven Local pick it up.

### Configure an app

The examples below use a project named `notes`, with Kotlin packages under `example`.
Use JDK 21 and a Gradle 9.5.1 wrapper in the app project.

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "notes"
```

Zoned uses a [kotlinx.html fork](https://github.com/reubenfirmin/kotlinx-html-new)
with typed DOM events and `web.*` browser types. Its JVM and JS artifacts are transitive
dependencies, but your app needs the JitPack repository to resolve them.

`build.gradle.kts`:

```kotlin
plugins {
    kotlin("multiplatform") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
    id("io.4rc.zoned.plugin") version "1.0-SNAPSHOT"
}

kotlin {
    jvmToolchain(21)
    jvm()
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "${project.name}.bundle.js"
                cssSupport { enabled.set(true) }
            }
        }
        binaries.executable()
    }
    sourceSets {
        commonMain.dependencies {
            implementation("io.4rc:zoned:1.0-SNAPSHOT")
        }
        jvmMain { kotlin.srcDir("build/generated/kotlin") }
        jsMain { kotlin.srcDir("build/generated/kotlin-js") }
    }
}

tasks.register<Copy>("copyBundle") {
    dependsOn("jsBrowserDistribution")
    from("build/dist/js/productionExecutable")
    into("dist")
}

tasks.register<Copy>("copyResources") {
    from("src/jvmMain/resources/assets")
    into("dist")
}

tasks.register<JavaExec>("run") {
    dependsOn("copyBundle", "copyResources", "jvmMainClasses")
    mainClass.set("example.MainKt")
    val main = kotlin.jvm().compilations.getByName("main")
    classpath(main.output, main.runtimeDependencyFiles)
}
```

The common dependency resolves to Zoned's JVM and JS artifacts for each target.
Declare additional npm dependencies in `jsMain.dependencies` using `implementation(npm(...))`.

The bundle filename matches the plugin's generated path, `/static/notes.bundle.js`.
The copy tasks put the bundle, lazy-loaded chunks, and assets in `dist/`; the server
below serves that directory at `/static`.
`bundleInit()` reads the plugin's generated bundle properties and adds the script tag.
For deployment, ship `dist/` alongside your server and run it from that directory's parent.

Use this source layout:

```text
src/commonMain/kotlin/example/        shared models and enhancement definitions
src/jvmMain/kotlin/example/           server entry point, routes, HTML, database code
src/jvmMain/resources/assets/         static files copied to dist/
src/jvmMain/resources/db/migration/   Flyway SQL migrations
src/jsMain/kotlin/example/            browser entry point and enhancement implementations
```

## Recipes

### Serve a page and update one fragment

Create `src/jvmMain/kotlin/example/Main.kt`:

```kotlin
package example

import java.time.Instant
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import zoned.framework.api.*
import zoned.framework.auth.Role
import zoned.framework.ui.libs.Bundle.bundleInit

class HomeApi : Api {
    override val basePath = ""
    override val baseRoles = emptyList<Role>()

    @GET("/")
    fun index(): Response = response {
        createHTML().html {
            head {
                title { +"Notes" }
                bundleInit()
            }
            body {
                button {
                    attributes["hx-get"] = route(this@HomeApi::time).url()
                    attributes["hx-target"] = "#server-time"
                    +"Get server time"
                }
                div { id = "server-time" }
            }
        }
    }

    @GET("/time")
    fun time(): Response = response {
        createHTML().p { +Instant.now().toString() }
    }
}

fun main() {
    Zoned.create {
        apis(HomeApi())
        staticFiles("/static", System.getProperty("user.dir") + "/dist")
    }.start(9000)
}
```

Create `src/jsMain/kotlin/example/Main.kt`:

```kotlin
package example

import zoned.enhancements.ZonedEnhancementRegistry
import zoned.framework.libs.HTMXHelper

fun main() {
    HTMXHelper.setupHTMX {
        ZonedEnhancementRegistry.initialize()
    }
}
```

Run `./gradlew run` and open <http://localhost:9000>. The button requests `/time` and
puts its HTML response inside `#server-time`. `route(this@HomeApi::time)` looks up the
annotated handler, so changing its path also changes the button's URL.

The HTMX callback initializes enhancements on page load and after content swaps.
`ZonedEnhancementRegistry` handles the enhancements supplied by the framework.

### Add a tooltip to server-rendered content

In `jvmMain`, use the generated wrapper function around the content you want to enhance:

```kotlin
import kotlinx.html.FlowContent
import kotlinx.html.span
import zoned.enhancements.tooltip

fun FlowContent.saveHint() {
    tooltip({ text = "Changes are saved automatically" }) {
        span { +"Saved" }
    }
}
```

Call `saveHint()` inside the page's `body` block. The wrapper contains the original HTML
and a serialized config; the registry from the previous recipe attaches the tooltip behavior.

For your own enhancement, add a `@ClientEnhancement` object and a `@Serializable` config
in `commonMain`. Give the config default values and mutable (`var`) properties for the DSL.
In `jsMain`, annotate the implementation with `@EnhancementImpl(YourEnhancement::class)`
and use this signature:

```kotlin
fun TagConsumer<HTMLElement>.initYourEnhancement(config: YourConfig, children: List<Node>)
```

`TagConsumer` comes from `kotlinx.html`, `HTMLElement` from `web.html`, and `Node` from
`web.dom`. Use `insertChildren(children)` from `zoned.framework.dom` inside the client
wrapper to preserve the server's content. The [tooltip implementation](src/jsMain/kotlin/zoned/framework/ui/enhancements/TooltipEnhancementImpl.kt)
shows the complete pattern.

Compilation generates your server DSL and client registry. For the `notes` project,
also call `notes.enhancements.NotesEnhancementRegistry.initialize()` in the HTMX callback.
The [rendering guide](CLAUDE.md#rendering-model-kotlinxhtml--elementtrackingconsumer)
covers DOM references, event handlers, and mount callbacks.

### Mount a Markdown editor

In `jsMain`, pass a mounted container and a save callback to this helper:

```kotlin
import kotlin.js.Promise
import kotlinx.css.Color
import kotlinx.css.px
import web.html.HTMLElement
import zoned.framework.libs.*

fun mountEditor(
    host: HTMLElement,
    initialText: String,
    save: (String) -> Unit,
): Promise<CodeMirrorWrapper> = loadCodeMirror().then { modules ->
    CodeMirrorWrapper(
        modules,
        host,
        CodeMirrorOptions(
            text = initialText,
            markdown = true,
            lineWrapping = true,
            theme = CodeMirrorTheme(
                background = Color("#fafafa"),
                fontSize = 16.px,
            ),
        ),
    ).also { editor ->
        editor.addKeyBinding("Mod-s") {
            save(it.getValue())
            true
        }
        editor.focus()
    }
}
```

Capture the container with `Ref<HTMLElement>()` and call the helper from `onMount` when
building it with Zoned's DOM DSL. Keep the returned editor and call `destroy()` when
removing it; that disposes both its view and stylesheet. The modules load on demand.

Use `plain = true` to hide line numbers and the active-line highlight, or `markdown = false`
for plain text. `setCompletionSource` adds application suggestions. Cursor positions use
zero-based rows and columns. `setValue` resets undo history when the text changes;
`insert` and `replace` make undoable edits. `view` and `modules` expose the typed native
APIs when you need an extension the wrapper doesn't cover.

### Migrate a local SQLite database and generate models

In the app's `.env`:

```dotenv
DB_PATH=notes.db
```

Create `src/jvmMain/resources/db/migration/V1__create_note.sql`:

```sql
CREATE TABLE note (
    id UUID PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    body TEXT NOT NULL
);
```

Create the model package directory, then migrate and generate in that order:

```bash
mkdir -p src/jvmMain/kotlin/example/model
./gradlew db-migrate
./gradlew model-generate
```

The generator finds the `model` directory and writes jOOQ sources under `example.model.jooq`.
Add a new numbered migration for each schema change, then repeat the two Gradle commands.

For PostgreSQL, omit `DB_PATH` and set `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and
`DB_PASS` in `.env`. The same migration and generation tasks apply.

## Browser-only apps

For a Kotlin/JS app without a server, keep the JS target and common dependency from the
build above; remove `jvm()`, the `jvmMain` source configuration, and the `copyBundle`,
`copyResources`, and `run` tasks.
Put your entry point in `src/jsMain/kotlin` and your `index.html` in `src/jsMain/resources`.
Have the HTML load `notes.bundle.js` with `defer` so the body exists before `main()` runs.

Build views with kotlinx.html and Zoned's `addToBody` or `appendTo` helpers. Client routing
is available through [`Routes` and `Router`](src/jsMain/kotlin/zoned/framework/routing).

```bash
./gradlew jsBrowserDevelopmentRun       # browser dev server
./gradlew jsBrowserProductionWebpack    # production bundle
```

## Development commands

| Command | Purpose |
| --- | --- |
| `./gradlew generate-watch-script` | Generate the app's `watch.sh` and `test.sh` scripts |
| `./watch.sh` | Rebuild and restart on changes; requires Bash and tmux |
| `./test.sh` | Run JVM/browser tests on a separate compiler daemon while watch is running |
| `./gradlew generate-enhancements` | Regenerate enhancement DSLs and registries; also runs before compilation |
| `./gradlew build-style` | Compile `style.css` from JVM resources (or JS resources) to `dist/output.css` |
| `./gradlew publishToMavenLocal` | Publish framework changes for dependent apps |

The watch script expects a `style.css` file in the app's resources and runs `build-style`
alongside compilation. Start it after setting up that file; the minimal example above
can run directly with `./gradlew run`. Link `/static/output.css` in your page head if
you use the generated stylesheet. Build failures are recorded in `.build_errors`.

To change the generated development scripts, edit
[`ZonedPlugin.kt`](gradle-plugin/src/main/kotlin/zoned/gradle/ZonedPlugin.kt) and regenerate them.
The framework test suite runs with `./test.sh`; browser tests need a browser installed.

## License

[MIT](LICENSE).

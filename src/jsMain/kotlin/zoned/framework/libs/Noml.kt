package zoned.framework.libs

import kotlin.js.Promise
import web.html.HTMLCanvasElement

/** The nomnoml module surface. */
external interface NomnomlModule {
    fun draw(canvas: HTMLCanvasElement, source: String)
}

private var nomnomlModule: NomnomlModule? = null

/**
 * Load nomnoml ON DEMAND — a dynamic `import()`, so webpack splits it out of the main bundle and
 * its parse cost is paid only when a card actually contains a diagram. Cached after first load.
 */
fun loadNomnoml(): Promise<NomnomlModule> {
    nomnomlModule?.let { return Promise.resolve(it) }
    return js("import('nomnoml')").unsafeCast<Promise<dynamic>>().then<NomnomlModule> { m ->
        val noml = unwrapModule(m, probe = "draw").unsafeCast<NomnomlModule>()
        nomnomlModule = noml
        noml
    }
}

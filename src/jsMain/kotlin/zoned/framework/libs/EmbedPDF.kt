package zoned.framework.libs

import js.objects.unsafeJso
import web.dom.Element

/**
 * Kotlin bindings for EmbedPDF viewer.
 *
 * Load via script tag: <script src="https://snippet.embedpdf.com/embedpdf.js"></script>
 *
 * Usage:
 * ```kotlin
 * val viewer = EmbedPDF.init(jso {
 *     type = "container"
 *     target = document.getElementById("pdf-viewer")
 *     src = "/documents/123/download"
 * })
 * ```
 */
@JsName("EmbedPDF")
external object EmbedPDF {
    fun init(options: EmbedPDFOptions): EmbedPDFInstance
}

external interface EmbedPDFOptions {
    /** Viewer type - typically "container" */
    var type: String?

    /** DOM element to render the viewer into */
    var target: Element?

    /** URL of the PDF to display */
    var src: String?
}

external interface EmbedPDFInstance {
    /** Load a different PDF */
    fun load(src: String)

    /** Destroy the viewer instance */
    fun destroy()
}

/**
 * Helper function to create a PDF viewer with common defaults.
 */
fun createPdfViewer(container: Element, pdfUrl: String): EmbedPDFInstance {
    return EmbedPDF.init(unsafeJso {
        type = "container"
        target = container
        src = pdfUrl
    })
}

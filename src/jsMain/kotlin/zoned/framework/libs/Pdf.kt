package zoned.framework.libs

import js.objects.unsafeJso
import kotlin.js.Promise

/** Small typed surface for jsPDF's text/vector API; no HTML screenshot dependencies. */
external interface JsPdf {
    fun setFont(name: String, style: String = definedExternally): JsPdf
    fun setFontSize(size: Double): JsPdf
    fun setTextColor(color: String): JsPdf
    fun setDrawColor(color: String): JsPdf
    fun setFillColor(color: String): JsPdf
    fun setLineWidth(width: Double): JsPdf
    fun text(text: String, x: Double, y: Double): JsPdf
    fun splitTextToSize(text: String, width: Double): Array<String>
    fun getTextWidth(text: String): Double
    fun line(x1: Double, y1: Double, x2: Double, y2: Double): JsPdf
    fun rect(x: Double, y: Double, width: Double, height: Double, style: String): JsPdf
    fun link(x: Double, y: Double, width: Double, height: Double, options: PdfLinkOptions)
    fun addPage(): JsPdf
    fun getNumberOfPages(): Int
    fun setPage(page: Int): JsPdf
    fun setProperties(properties: PdfProperties): JsPdf
    fun save(filename: String): JsPdf
}

external interface PdfLinkOptions { var url: String }
external interface PdfProperties { var title: String; var author: String }
external interface PdfOptions {
    var unit: String
    var format: String
    var compress: Boolean
    var putOnlyUsedFonts: Boolean
}

private external interface PdfFactory { fun create(options: PdfOptions): JsPdf }
private var pdfFactory: Promise<PdfFactory>? = null

/**
 * Load jsPDF on first use. Cache the module, but create a fresh document for every call.
 * The sole JS expression is the dynamic import/constructor bridge, as in loadAce(); Kotlin
 * has no code-split import expression. All options and the returned document stay typed.
 */
fun createPdf(): Promise<JsPdf> {
    val loading = pdfFactory ?: (js(
        "import('jspdf').then(function(m) { return { create: function(options) { return new m.jsPDF(options); } }; })"
    ) as Promise<PdfFactory>).catch {
        pdfFactory = null
        throw it
    }.also { pdfFactory = it }
    return loading.then { factory ->
        factory.create(unsafeJso<PdfOptions> {
            unit = "pt"
            format = "a4"
            compress = true
            putOnlyUsedFonts = true
        })
    }
}

fun pdfLink(url: String): PdfLinkOptions = unsafeJso { this.url = url }
fun pdfProperties(title: String, author: String): PdfProperties = unsafeJso {
    this.title = title
    this.author = author
}

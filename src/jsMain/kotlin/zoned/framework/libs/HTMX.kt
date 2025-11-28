package zoned.framework.libs

import js.objects.Record
import js.objects.jso
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.parsing.DOMParser
import org.w3c.xhr.XMLHttpRequest
import web.dom.Element

// this is here for the initial import - see https://htmx.org/docs/#webpack
@JsModule("htmx.org/dist/htmx.esm.js")
@JsNonModule
external object HTMXModule {
    val default: HTMX
}

external interface HTMX {
    fun onLoad(handler: (Element) -> Unit)
    fun on(eventName: String, handler: (HTMXEvent) -> Unit)

    /**
     * Trigger an HTMX ajax request programmatically.
     * @param method HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param url The URL to request
     * @param options Object with target, swap, values, etc.
     */
    fun ajax(method: String, url: String, options: dynamic)

    /**
     * Process an element and its children for HTMX attributes.
     * Call this after dynamically adding elements that have hx-* attributes.
     */
    fun process(element: Element)
}

/**
 * Options for htmx.ajax() calls
 */
external interface HTMXAjaxOptions {
    var target: String?
    var swap: String?
    var values: Record<String, String>?
}

/**
 * HTMX custom event with typed detail
 */
external interface HTMXEvent {
    val detail: HTMXEventDetail
}

external interface HTMXEventDetail {
    val elt: Element?        // htmx:load
    val target: Element?     // htmx:afterSwap
    val xhr: XMLHttpRequest? // htmx:afterSwap
}

/**
 * Extension to attach htmx to window (required per https://htmx.org/docs/#webpack)
 */
external interface WindowWithHTMX {
    var htmx: HTMX?
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
fun attachHTMXToWindow(htmx: HTMX) {
    (window as WindowWithHTMX).htmx = htmx
}

/**
 * Convert a Kotlin Map to a typed JS Record for HTMX form values
 */
fun Map<String, String>.toRecord(): Record<String, String> {
    val record: Record<String, String> = jso()
    this.forEach { (k, v) -> record[k] = v }
    return record
}

object HTMXHelper {
    lateinit var htmx: HTMX

    fun setupHTMX(callback: () -> Unit = {}) {
        htmx = HTMXModule.default
        // we also need to attach it to the window per https://htmx.org/docs/#webpack
        attachHTMXToWindow(htmx)

        // and then we can wire up our onLoad per https://htmx.org/docs/#init_3rd_party_with_events
        htmx.on("htmx:load") { event ->
            val content = event.detail.elt ?: return@on
            // Use selective Flowbite initialization to avoid initCollapses() which adds resize listeners
            // that interfere with Tailwind CSS responsive utilities
            initDropdowns()
            initModals()
            initTooltips()
            initAccordions()
            FlowbiteHelpers.clearOpenElements()

            // since body is boosted, we don't rerun all of head each time. but we do want to change the title between pages
            val title = content.querySelector("head > title")

            if (title != null) {
                title.textContent?.let { newTitle ->
                    document.title = newTitle
                }
            }
            callback()
        }
        
        // https://github.com/bigskysoftware/htmx/issues/1384
        htmx.on("htmx:afterSwap") { event ->
            val parser = DOMParser()
            val detail = event.detail
            val targetEl = detail.target ?: return@on
            val xhr = detail.xhr ?: return@on

            if (targetEl.tagName == "BODY") {
                val response = xhr.responseText
                val parsedResponse = parser.parseFromString(response, "text/html")
                val bodyAttributes = parsedResponse.getElementsByTagName("body").item(0)!!.attributes

                // remove old
                for (i in (0 until targetEl.attributes.length).reversed()) {
                    val attr = targetEl.attributes.item(i)!!
                    targetEl.removeAttribute(attr.name)
                }

                // set new
                for (i in 0 until bodyAttributes.length) {
                    val attr = bodyAttributes.item(i)!!
                    targetEl.setAttribute(attr.name, attr.value)
                }
            }
        }

        setupGlobalErrorHandler()
    }

    private fun setupGlobalErrorHandler() {
        val originalOnError = window.onerror

        window.onerror = { message, source, lineNo, colNo, error ->
            if (error is Throwable) {
                val errorString = error.toString()
                val stackTrace = error.stackTraceToString()

                if (errorString.contains("TypeError: Cannot read properties of null (reading 'classList')") &&
                    stackTrace.contains("htmx.org")) {

                    console.warn("Suppressed HTMX classList error:", errorString)
                    console.warn("Error occurred at:", source, lineNo, colNo)
                    console.warn("Stack trace:", stackTrace)

                    // Return true to indicate that the error has been handled
                    true
                } else {
                    // For other errors, call the original error handler if it exists
                    if (originalOnError != null) {
                        console.log("Calling original 1")
                        originalOnError(message, source, lineNo, colNo, error)
                    } else {
                        // If no original handler, log the error and rethrow
                        console.error("Uncaught error:", error)
                        false // Allows the error to propagate
                    }
                }
            } else {
                // For non-Error objects, call the original error handler or log and rethrow
                if (originalOnError != null) {
                    console.log("Calling original 2")
                    originalOnError(message, source, lineNo, colNo, error)
                } else {
                    console.error("Uncaught error:", message)
                    false // Allows the error to propagate
                }
            }
        }
    }

    /**
     * Trigger an HTMX POST request.
     * @param url The URL (from server-serialized config)
     * @param target CSS selector for response target
     * @param values Form data as key-value pairs
     * @param swap HTMX swap strategy (default: innerHTML)
     */
    fun post(url: String, target: String, values: Map<String, String> = emptyMap(), swap: String = "innerHTML") {
        htmx.ajax("POST", url, jso<HTMXAjaxOptions> {
            this.target = target
            this.swap = swap
            if (values.isNotEmpty()) {
                this.values = values.toRecord()
            }
        })
    }

    /**
     * Trigger an HTMX GET request.
     * @param url The URL (from server-serialized config)
     * @param target CSS selector for response target
     * @param swap HTMX swap strategy (default: innerHTML)
     */
    fun get(url: String, target: String, swap: String = "innerHTML") {
        htmx.ajax("GET", url, jso<HTMXAjaxOptions> {
            this.target = target
            this.swap = swap
        })
    }

    /**
     * Trigger an HTMX DELETE request.
     * @param url The URL (from server-serialized config)
     * @param target CSS selector for response target
     * @param swap HTMX swap strategy (default: innerHTML)
     */
    fun delete(url: String, target: String, swap: String = "innerHTML") {
        htmx.ajax("DELETE", url, jso<HTMXAjaxOptions> {
            this.target = target
            this.swap = swap
        })
    }

    /**
     * Trigger an HTMX PUT request.
     * @param url The URL (from server-serialized config)
     * @param target CSS selector for response target
     * @param values Form data as key-value pairs
     * @param swap HTMX swap strategy (default: innerHTML)
     */
    fun put(url: String, target: String, values: Map<String, String> = emptyMap(), swap: String = "innerHTML") {
        htmx.ajax("PUT", url, jso<HTMXAjaxOptions> {
            this.target = target
            this.swap = swap
            if (values.isNotEmpty()) {
                this.values = values.toRecord()
            }
        })
    }
}
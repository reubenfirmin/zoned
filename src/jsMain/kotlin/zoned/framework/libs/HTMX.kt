package zoned.framework.libs

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.events.Event
import org.w3c.dom.parsing.DOMParser

// this is here for the initial import - see https://htmx.org/docs/#webpack
@JsModule("htmx.org/dist/htmx.esm.js")
@JsNonModule
external object HTMXModule {
    val default: HTMX
}

external interface HTMX {
    fun onLoad(handler: (Element) -> Unit)
    fun on(eventName: String, handler: (Event) -> Unit)
}

object HTMXHelper {
    lateinit var htmx: HTMX

    fun setupHTMX(callback: () -> Unit = {}) {
        htmx = HTMXModule.default
        // we also need to attach it to the window per https://htmx.org/docs/#webpack
        window.asDynamic().htmx = htmx

        // and then we can wire up our onLoad per https://htmx.org/docs/#init_3rd_party_with_events
        htmx.on("htmx:load") { event ->
            val content = event.asDynamic().detail.elt as Element
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
            val evt = event.asDynamic()

            if (evt.detail.target.tagName == "BODY") {
                val parsedResponse = parser.parseFromString(evt.detail.xhr.response as String, "text/html");
                val bodyAttributes = parsedResponse.getElementsByTagName("body").item(0)!!.attributes
                val targetEl = evt.detail.target as Element

                // remove old
                for (i in (0 until targetEl.attributes.length).reversed()) {
                    val attr = targetEl.attributes.item(i)!!
                    targetEl.removeAttribute(attr.name)
                }

                // set new
                for (i in 0 until bodyAttributes.length) {
                    val attr = bodyAttributes.item(i)!!
                    evt.detail.target.setAttribute(attr.name, attr.value)
                }
            }
        }

        setupGlobalErrorHandler()
    }

    private fun setupGlobalErrorHandler() {
        val originalOnError = window.onerror

        window.onerror = { message, source, lineNo, colNo, error ->
            if (error is Error) {
                val errorString = error.toString()
                val stackTrace = error.asDynamic().stack as? String

                if (errorString.contains("TypeError: Cannot read properties of null (reading 'classList')") &&
                    stackTrace?.contains("htmx.org") == true) {

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
}
package zoned.framework.ui.testing

import io.javalin.http.Context
import kotlinx.html.CommonAttributeGroupFacade
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import zoned.framework.api.Response
import zoned.framework.ui.layouts.HTMXTarget
import java.io.StringWriter

/**
 * Testing utilities for HTMX-enhanced applications
 *
 * These utilities help verify HTMX attributes, targets, and behaviors in tests
 */
object HTMXTestUtils {

    /**
     * Parse HTML response into a Jsoup document for testing
     */
    fun parseResponse(response: Response): Document {
        val html = if (response.body.isLeft()) {
            response.body.left as String
        } else {
            throw IllegalArgumentException("Cannot parse JSON response as HTML")
        }
        return Jsoup.parse(html)
    }

    /**
     * Assert that an element with the given HTMX target exists
     */
    fun Document.assertTargetExists(target: HTMXTarget): Element {
        return getElementById(target.id)
            ?: throw AssertionError("Expected element with id='${target.id}' not found in document")
    }

    /**
     * Assert that an element has the expected HTMX attributes
     */
    fun Element.assertHtmxAttribute(attribute: String, expectedValue: String) {
        val actualValue = attr(attribute)
        if (actualValue != expectedValue) {
            throw AssertionError(
                "Expected HTMX attribute '$attribute' to be '$expectedValue' but was '$actualValue'"
            )
        }
    }

    /**
     * Assert that an element has an hx-get attribute with the expected path
     */
    fun Element.assertHtmxGet(expectedPath: String) {
        assertHtmxAttribute("hx-get", expectedPath)
    }

    /**
     * Assert that an element has an hx-post attribute with the expected path
     */
    fun Element.assertHtmxPost(expectedPath: String) {
        assertHtmxAttribute("hx-post", expectedPath)
    }

    /**
     * Assert that an element has an hx-target attribute
     */
    fun Element.assertHtmxTarget(target: HTMXTarget) {
        assertHtmxAttribute("hx-target", target.selector)
    }

    /**
     * Assert that an element has an hx-swap attribute
     */
    fun Element.assertHtmxSwap(swapStrategy: String) {
        assertHtmxAttribute("hx-swap", swapStrategy)
    }

    /**
     * Assert that an element has an hx-trigger attribute
     */
    fun Element.assertHtmxTrigger(trigger: String) {
        assertHtmxAttribute("hx-trigger", trigger)
    }

    /**
     * Get all elements with HTMX attributes
     */
    fun Document.findHtmxElements(): List<Element> {
        return select("[hx-get], [hx-post], [hx-put], [hx-delete], [hx-patch]")
    }

    /**
     * Find all HTMX targets in the document
     */
    fun Document.findHtmxTargets(): List<Element> {
        return select("[hx-target]")
    }

    /**
     * Assert that a context has the expected HTMX response header
     */
    fun assertHtmxHeader(headers: Map<String, String>, headerName: String, expectedValue: String) {
        val actualValue = headers[headerName]
        if (actualValue != expectedValue) {
            throw AssertionError(
                "Expected HTMX header '$headerName' to be '$expectedValue' but was '$actualValue'"
            )
        }
    }

    /**
     * Verify HTMX headers in a map (useful for testing response headers)
     */
    fun verifyHtmxLocation(headers: Map<String, String>, expectedPath: String) {
        assertHtmxHeader(headers, "HX-Location", expectedPath)
    }

    fun verifyHtmxRedirect(headers: Map<String, String>, expectedPath: String) {
        assertHtmxHeader(headers, "HX-Redirect", expectedPath)
    }

    fun verifyHtmxRefresh(headers: Map<String, String>) {
        assertHtmxHeader(headers, "HX-Refresh", "true")
    }

    fun verifyHtmxRetarget(headers: Map<String, String>, expectedTarget: String) {
        assertHtmxHeader(headers, "HX-Retarget", expectedTarget)
    }

    fun verifyHtmxReswap(headers: Map<String, String>, expectedSwap: String) {
        assertHtmxHeader(headers, "HX-Reswap", expectedSwap)
    }

    fun verifyHtmxTrigger(headers: Map<String, String>, expectedEvents: String) {
        assertHtmxHeader(headers, "HX-Trigger", expectedEvents)
    }
}

/**
 * Extension functions for easier testing
 */

/**
 * Assert that this response renders the given HTMX target
 */
fun Response.assertRendersTarget(target: HTMXTarget) {
    val doc = HTMXTestUtils.parseResponse(this)
    HTMXTestUtils.run { doc.assertTargetExists(target) }
}

/**
 * Assert that this response has the given HTMX target header
 */
fun Response.assertTargetsElement(expectedTarget: HTMXTarget) {
    val actualTarget = this.target
    if (actualTarget?.id != expectedTarget.id) {
        throw AssertionError(
            "Expected response to target '${expectedTarget.id}' but targeted '${actualTarget?.id}'"
        )
    }
}

/**
 * Parse this response as HTML for testing
 */
fun Response.toDocument(): Document {
    return HTMXTestUtils.parseResponse(this)
}

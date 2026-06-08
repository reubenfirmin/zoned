package zoned.framework.routing

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlinx.html.TagConsumer
import web.html.HTMLElement

private typealias TagConsumerHandler = TagConsumer<HTMLElement>.(Params) -> Any

class RouteTrieTest {

    @BeforeTest fun setup() = RouteTrie.clear()
    @AfterTest fun teardown() = RouteTrie.clear()

    private fun noop(): TagConsumerHandler = { _ -> Unit }

    @Test
    fun `static sibling wins over wildcard`() {
        RouteCreator.addRoute("/help/shortcuts", handler = noop())
        RouteCreator.addRoute("/{canvas...}", handler = noop())

        val help = RouteTrie.findRoute("/help/shortcuts")
        assertNotNull(help)
        assertEquals("/help/shortcuts", help.first.pattern)

        val canvas = RouteTrie.findRoute("/my/board")
        assertNotNull(canvas)
        assertEquals("/{canvas...}", canvas.first.pattern)
        assertEquals("my/board", canvas.second["canvas"])
    }

    @Test
    fun `wildcard still matches single segment when no static sibling`() {
        RouteCreator.addRoute("/{canvas...}", handler = noop())
        val canvas = RouteTrie.findRoute("/standalone")
        assertNotNull(canvas)
        assertEquals("standalone", canvas.second["canvas"])
    }

    @Test
    fun `reserved prefix with no terminal route does not fall through to wildcard`() {
        RouteCreator.addRoute("/help/shortcuts", handler = noop())
        RouteCreator.addRoute("/{canvas...}", handler = noop())
        // "/help" matches the static `help` branch but has no RouteNode there -> not found
        // (it must NOT be reinterpreted as a canvas named "help").
        assertNull(RouteTrie.findRoute("/help"))
    }

    @Test
    fun `title metadata computes document title from params`() {
        val route = RouteCreator.addRoute("/help/{topic}", handler = noop())
            .title { p -> "Help: ${p["topic"]}" }
        val title = route.metadata.title
        assertNotNull(title)
        assertEquals("Help: shortcuts", title(Params(mapOf("topic" to "shortcuts"))))
    }
}

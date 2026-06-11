package zoned.framework.routing

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RoutesMountingTest {

    private class Pages {
        fun greet() {}
    }

    @BeforeTest fun setup() = RouteTrie.clear()
    @AfterTest fun teardown() = RouteTrie.clear()

    @Test
    fun collectionRoutesAreNotInTheTrieUntilMounted() {
        val pages = object : Routes<Pages>(Pages(), "/mount-a") {
            val main = route(mode = RenderMode.PARTIAL) { "/main" to { greet() } }
        }
        assertNull(RouteTrie.findRoute("/mount-a/main"), "defining a collection must not register routes")
        Router.mount(pages)
        assertNotNull(RouteTrie.findRoute("/mount-a/main"), "mounting registers the collection's routes")
    }

    @Test
    fun pathFailsLoudlyBeforeMountAndResolvesAfter() {
        val pages = object : Routes<Pages>(Pages(), "/mount-b") {
            val main = route(mode = RenderMode.PARTIAL) { "/main" to { greet() } }
        }
        assertFails("a typed link to an unmounted collection must fail with a clear error") {
            pages.main.path()
        }
        Router.mount(pages)
        assertEquals("/mount-b/main", pages.main.path())
    }

    @Test
    fun mountingTwiceDoesNotDuplicate() {
        val pages = object : Routes<Pages>(Pages(), "/mount-c") {
            val main = route(mode = RenderMode.PARTIAL) { "/main" to { greet() } }
        }
        Router.mount(pages)
        Router.mount(pages)
        assertNotNull(RouteTrie.findRoute("/mount-c/main"))
    }

    @Test
    fun standaloneAddRouteStillMountsImmediately() {
        // The non-collection escape hatch (used by tests and quick prototypes) keeps its semantics.
        RouteCreator.addRoute("/standalone-now", handler = { })
        assertNotNull(RouteTrie.findRoute("/standalone-now"))
    }

    @Test
    fun fragmentPathComposesFromBasePathNotParent() {
        val parentPages = object : Routes<Pages>(Pages(), "/") {
            val home = route(mode = RenderMode.PARTIAL) { "/" to { greet() } }
        }
        val overlayPages = object : Routes<Pages>(Pages(), "/helpish") {
            val topic = fragment(Zone("mount-test-zone"), parentPages.home, mode = RenderMode.PARTIAL) {
                "/topic" to { greet() }
            }
        }
        Router.mount(parentPages, overlayPages)
        assertNotNull(RouteTrie.findRoute("/helpish/topic"), "fragment lives at basePath + path")
        assertEquals("/helpish/topic", overlayPages.topic.path())
    }
}

class RoutePathFillTest {

    private object WildcardPages : Routes<Unit>(Unit, "/rpf-wild") {
        val board = route(mode = RenderMode.PARTIAL) { "/{canvas...}" to { } }
        val mixed = route(mode = RenderMode.PARTIAL) { "/a/{id}/{rest...}" to { } }
    }

    @Test
    fun pathFillsWildcardSegments() {
        Router.mount(WildcardPages)
        assertEquals("/rpf-wild/work", WildcardPages.board.path("work"))
        assertEquals("/rpf-wild/a/7/x", WildcardPages.mixed.path("7", "x"))
    }

    @Test
    fun pathRejectsWrongArityIncludingWildcards() {
        Router.mount(WildcardPages)
        assertFailsWith<IllegalArgumentException> { WildcardPages.board.path() }
        assertFailsWith<IllegalArgumentException> { WildcardPages.mixed.path("7") }
    }
}

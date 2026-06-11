package zoned.framework.routing

import kotlinx.html.TagConsumer
import kotlinx.html.div
import kotlinx.html.id
import web.dom.ElementId
import web.dom.document
import web.html.HTMLElement
import zoned.framework.events.EventBus
import zoned.framework.hasDom
import zoned.framework.interop.addToBody
import kotlin.test.Test
import kotlin.test.assertEquals

private interface VEvent
private object Poke : VEvent

class ZoneViewTest {

    private class TestView(zone: Zone, bus: EventBus<VEvent>) : ZoneView<String, VEvent>(zone, bus) {
        var pokes = 0
        var disposals = 0
        override fun TagConsumer<HTMLElement>.render(model: String) {
            on<Poke> { pokes++ }
            onDispose { disposals++ }
            div { id = "zv-content"; +model }
        }
    }

    private fun EventBus<VEvent>.totalSubscriptions() = eventRegistry.values.sumOf { it.size }

    @Test
    fun swapReplacesContentAndDetachesTheOutgoingSubscriptions() {
        if (!hasDom) return
        addToBody { div { id = "zv-zone" } }
        val bus = EventBus<VEvent>()
        val view = TestView(Zone("zv-zone"), bus)
        try {
            view.swap("one")
            assertEquals(1, bus.totalSubscriptions(), "first render subscribes once")
            assertEquals("one", document.getElementById(ElementId("zv-content"))?.textContent)

            view.swap("two")
            assertEquals(1, bus.totalSubscriptions(), "swap detaches the outgoing content's subscription")
            assertEquals(1, view.disposals, "onDispose ran for the outgoing content")
            assertEquals("two", document.getElementById(ElementId("zv-content"))?.textContent)

            bus.fire(Poke)
            assertEquals(1, view.pokes, "only the live content's handler fires")

            view.dispose()
            assertEquals(0, bus.totalSubscriptions(), "dispose detaches everything")
            view.dispose()
            assertEquals(2, view.disposals, "dispose is idempotent (cleanups run once)")
        } finally {
            document.getElementById(ElementId("zv-zone"))?.remove()
        }
    }
}

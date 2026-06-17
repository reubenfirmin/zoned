package zoned.framework.events

import kotlinx.browser.window
import kotlinx.html.div
import kotlinx.html.id
import web.dom.ElementId
import web.dom.document
import web.html.HTMLElement
import zoned.framework.hasDom
import zoned.framework.interop.addToBody
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertEquals

private interface TestEvent
private data class Ping(val n: Int) : TestEvent
private data class Pong(val s: String) : TestEvent

class EventBusTest {

    @Test
    fun firesGlobalSubscribers() {
        val bus = EventBus<TestEvent>()
        var got = 0
        bus.on<Ping> { got = it.n }
        bus.fire(Ping(7))
        assertEquals(7, got)
    }

    @Test
    fun offRemovesTheExactHandler() {
        val bus = EventBus<TestEvent>()
        var count = 0
        val handler: (Ping) -> Unit = { count++ }
        bus.on(handler)
        bus.fire(Ping(1))
        bus.off(handler)
        bus.fire(Ping(1))
        assertEquals(1, count)
    }

    @Test
    fun dispatchIsPerConcreteType() {
        val bus = EventBus<TestEvent>()
        var pings = 0
        var pongs = 0
        bus.on<Ping> { pings++ }
        bus.on<Pong> { pongs++ }
        bus.fire(Ping(1))
        bus.fire(Pong("x"))
        assertEquals(1, pings)
        assertEquals(1, pongs)
    }

    @Test
    fun eventsFiredDuringDispatchRunAfterwardsInOrder() {
        val bus = EventBus<TestEvent>()
        val order = mutableListOf<String>()
        bus.on<Ping> { order += "ping"; bus.fire(Pong("nested")) }
        bus.on<Pong> { order += "pong" }
        bus.fire(Ping(1))
        assertEquals(listOf("ping", "pong"), order)
    }

    @Test
    fun elementScopedSubscriptionPrunesOnRemoval(): Promise<Unit> {
        if (!hasDom) return Promise.resolve(Unit)
        val bus = EventBus<TestEvent>()
        var count = 0
        addToBody { div { id = "bus-el-1" } }
        val el = document.getElementById(ElementId("bus-el-1")) as HTMLElement
        with(bus) { el.on<Ping> { count++ } }
        bus.fire(Ping(1))
        el.remove()
        return Promise { resolve, reject ->
            window.setTimeout({
                bus.fire(Ping(2))
                if (count == 1) resolve(Unit)
                else reject(AssertionError("subscription not pruned after element removal: count=$count"))
            }, 20)
        }
    }

    @Test
    fun elementScopedSubscriptionRegisteredBeforeAttachAlsoPrunes(): Promise<Unit> {
        if (!hasDom) return Promise.resolve(Unit)
        // Registering before the element has a parent must neither leak the subscription forever
        // nor prune it prematurely while it is attached.
        val bus = EventBus<TestEvent>()
        var count = 0
        val el = document.createElement("div")
        with(bus) { el.on<Ping> { count++ } }
        document.body.appendChild(el)
        bus.fire(Ping(1))               // attached: handler fires
        el.remove()
        return Promise { resolve, reject ->
            window.setTimeout({
                bus.fire(Ping(2))       // removed: handler pruned
                if (count == 1) resolve(Unit)
                else reject(AssertionError("pre-attach registration leaked or pruned early: count=$count"))
            }, 20)
        }
    }

    @Test
    fun ancestorRemovalPrunesElementScopedSubscription(): Promise<Unit> {
        if (!hasDom) return Promise.resolve(Unit)
        val bus = EventBus<TestEvent>()
        var count = 0
        addToBody {
            div {
                id = "bus-outer"
                div { id = "bus-inner" }
            }
        }
        val inner = document.getElementById(ElementId("bus-inner")) as HTMLElement
        with(bus) { inner.on<Ping> { count++ } }
        document.getElementById(ElementId("bus-outer"))?.remove()   // remove ANCESTOR
        return Promise { resolve, reject ->
            window.setTimeout({
                bus.fire(Ping(1))
                if (count == 0) resolve(Unit)
                else reject(AssertionError("subscription survived ancestor removal: count=$count"))
            }, 20)
        }
    }
}

class EventBusResilienceTest {

    private interface Ev
    private class Boom : Ev
    private class After : Ev

    @Test
    fun busRecoversAfterAHandlerThrows() {
        val bus = EventBus<Ev>()
        bus.on<Boom> { throw IllegalStateException("handler exploded") }
        var afterRan = false
        bus.on<After> { afterRan = true }

        assertFailsWith<IllegalStateException> { bus.fire(Boom()) }

        // The poisoned event must not wedge the queue: later events still dispatch.
        bus.fire(After())
        assertTrue(afterRan, "an event fired after a throwing handler must still dispatch")
    }

    @Test
    fun eventsQueuedBehindAThrowingHandlerDispatchOnTheNextFire() {
        val bus = EventBus<Ev>()
        var afterRan = false
        bus.on<After> { afterRan = true }
        bus.on<Boom> {
            bus.fire(After())   // queued behind the in-flight Boom
            throw IllegalStateException("boom")
        }

        assertFailsWith<IllegalStateException> { bus.fire(Boom()) }
        // After was queued when Boom blew up; the next fire must drain it.
        bus.fire(Boom().let { object : Ev {} })   // unrelated event with no handlers
        assertTrue(afterRan, "events queued behind the throwing handler dispatch on the next fire")
    }
}

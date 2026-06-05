package zoned.framework.api

import io.javalin.Javalin

class Zoned private constructor(private val javalin: Javalin) {

    fun start(port: Int): Zoned {
        javalin.start(port)
        return this
    }

    fun stop(): Zoned {
        javalin.stop()
        return this
    }

    companion object {
        fun create(block: ZonedSpec.() -> Unit): Zoned {
            val spec = ZonedSpec().apply(block)
            val javalin = Javalin.create { config -> spec.applyTo(config) }
            return Zoned(javalin)
        }
    }
}

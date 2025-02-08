package zoned.framework.di

import com.google.inject.Guice
import com.google.inject.Injector
import dev.misfitlabs.kotlinguice4.getInstance

var injector: Injector? = null

fun set(init: KModule.() -> Unit) {
    val kmodule = object : KModule() {
        override fun configure() {
            init()
        }
    }
    injector = if (injector == null) {
        Guice.createInjector(kmodule)
    } else {
        injector!!.createChildInjector(kmodule)
    }
}

inline fun <reified T> get() = injector?.getInstance<T>() ?: throw Exception("Please initialized injector first")

package zoned.framework.di

import dev.misfitlabs.kotlinguice4.KotlinBinder

import com.google.inject.AbstractModule
import com.google.inject.Binder
import com.google.inject.binder.ConstantBindingBuilder
import com.google.inject.name.Names
import dev.misfitlabs.kotlinguice4.binder.KotlinAnnotatedBindingBuilder
import dev.misfitlabs.kotlinguice4.binder.KotlinAnnotatedElementBuilder
import dev.misfitlabs.kotlinguice4.binder.KotlinLinkedBindingBuilder
import dev.misfitlabs.kotlinguice4.binder.KotlinScopedBindingBuilder
import dev.misfitlabs.kotlinguice4.internal.KotlinBindingBuilder
import kotlin.reflect.KProperty

/**
 * Fork of misfit lab's KotlinModule, opening up access to bind to allow more
 * convenient dsl
 */
abstract class KModule : AbstractModule() {
    private class KotlinLazyBinder(private val delegateBinder: () -> Binder) {
        private val classesToSkip = arrayOf(
            KotlinAnnotatedBindingBuilder::class.java,
            KotlinAnnotatedElementBuilder::class.java,
            KotlinBinder::class.java,
            KotlinBindingBuilder::class.java,
            KotlinLinkedBindingBuilder::class.java,
            KotlinScopedBindingBuilder::class.java
        )

        var lazyBinder = lazyInit()
        var currentBinder: Binder? = null

        operator fun getValue(thisRef: Any?, property: KProperty<*>): KotlinBinder {
            if (currentBinder != delegateBinder()) {
                currentBinder = delegateBinder()
                lazyBinder = lazyInit()
            }
            return lazyBinder.value
        }

        private fun lazyInit() = lazy { KotlinBinder(delegateBinder().skipSources(*classesToSkip)) }
    }

    /** Gets direct access to the underlying [KotlinBinder]. */
    val kotlinBinder: KotlinBinder by KotlinLazyBinder { this.binder() }

    /** @see KotlinBinder.bind */
    inline fun <reified T> bind(): KotlinAnnotatedBindingBuilder<T> {
        return kotlinBinder.bind<T>()
    }

    /**
     * Bind a constant annotated with @Named
     */
    inline fun <reified T> bind(named: String): ConstantBindingBuilder {
        return kotlinBinder.bindConstant().annotatedWith(Names.named(named))
    }
}
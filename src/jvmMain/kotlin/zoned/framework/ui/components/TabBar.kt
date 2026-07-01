package zoned.framework.ui.components

import zoned.framework.ui.libs.components.htmx.WithOptions
import zoned.framework.ui.libs.components.htmx.htmxParamLink
import kotlinx.html.*

class TabBar(private val tabs: List<Tab>, consumer: TagConsumer<*>): DIV(mapOf(
    "class" to "block text-xl font-medium text-center text-gray-500 border-b border-gray-200 dark:text-gray-400 " +
            "dark:border-gray-700 pb-8"), consumer) {

    private val activeClasses = "inline-block p-4 text-primary-600 border-b-2 border-primary-600 rounded-t-lg " +
            "active dark:text-primary-500 dark:border-primary-500"

    private val inactiveClasses = "inline-block p-4 border-b-2 border-transparent rounded-t-lg " +
            "hover:text-gray-600 hover:border-gray-300 dark:hover:text-gray-300"

    fun render(block: TabBar.() -> Unit) {
        ul("flex flex-wrap -mb-px") {
            this@TabBar.tabs.forEach { tab ->
                li("me-2") {
                    htmxParamLink(
                        tab.path, tab.target, WithOptions(
                            params = tab.params,
                            attrs = if (tab.current) {
                                mapOf("aria-current" to "page")
                            } else {
                                mapOf()
                            },
                            classes = if (tab.current) {
                                this@TabBar.activeClasses
                            } else {
                                this@TabBar.inactiveClasses
                            }
                        )
                    ) {
                        +tab.label
                    }
                }
            }
        }
        block()
    }
}

fun FlowContent.tabBar(tabs: List<Tab>, block: TabBar.() -> Unit = {}) {
    TabBar(tabs, consumer).visit {
        render(block)
    }
}

data class Tab(val label: String,
               val current: Boolean,
               val path: String,
               val target: String,
               val params: Map<String, String>)
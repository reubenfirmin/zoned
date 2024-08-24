package zoned.framework.ui.components.navbar

import kotlinx.html.*
import zoned.framework.api.BaseRoute
import zoned.framework.ui.libs.HTMX.pushUrl

class NavbarLink(private val label: String,
                 private val action: BaseRoute,
                 private val current: Boolean,
                 consumer: TagConsumer<*>):
    LI(mapOf(), consumer) {

    private val activeClasses = "block py-2 px-3 text-white bg-blue-700 rounded md:bg-transparent md:text-blue-700 " +
            "md:p-0 dark:text-white md:dark:text-blue-500"

    private val inactiveClasses = "navbar-link block py-2 pl-3 pr-4 text-gray-900 rounded hover:bg-gray-100 " +
            "md:hover:bg-transparent md:border-0 md:hover:text-blue-700 md:p-0 dark:text-white " +
            "md:dark:hover:text-blue-500 dark:hover:bg-gray-700 dark:hover:text-white md:dark:hover:bg-transparent"

    fun render() {
        a(classes = if (current) activeClasses else inactiveClasses) {
            href=this@NavbarLink.action.path
            // TODO we're ignoring method
            +this@NavbarLink.label
        }
    }
}

fun UL.navbarLink(label: String, action: BaseRoute, current: Boolean) {
    NavbarLink(label, action, current, consumer).visit {
        this.render()
    }
}
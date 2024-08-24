package zoned.framework.ui.components.navbar

import kotlinx.html.*
import zoned.framework.api.BaseRoute
import zoned.framework.auth.Identity
import zoned.framework.ui.components.buttons.HTMXAction

class Navbar(val classes: String,
             val identity: Identity,
             val navigations: List<Pair<String, BaseRoute>>,
             val logoAction: HTMXAction,
             val profileMenuItems: List<ProfileMenuItem>,
             val current: BaseRoute, consumer : TagConsumer<*>):
    NAV(mapOf("class" to "w-screen bg-white border-gray-200 dark:bg-gray-900 $classes"), consumer) {

    fun render() {
        div("flex flex-wrap items-center justify-between mx-auto p-4") {
            this@Navbar.logo(this@Navbar.logoAction)
            this@Navbar.mobileMenu("navbar-default")
            div("hidden w-full md:block md:w-auto") {
                id = "navbar-default"
                ul("font-medium w-full flex flex-col p-4 md:p-0 mt-4 border border-gray-100 rounded-lg bg-gray-50 " +
                        "md:flex-row md:space-x-8 md:mt-0 md:border-0 md:bg-white dark:bg-gray-800 md:dark:bg-gray-900 " +
                        "dark:border-gray-700") {
                    this@Navbar.navigations.forEach {
                        navbarLink(it.first, it.second, it.second == this@Navbar.current)
                    }
                }
            }
            this@Navbar.profileMenu(this@Navbar.identity, this@Navbar.profileMenuItems)
        }
    }
}

fun FlowContent.navbar(classes: String,
                       user: Identity,
                       navigations: List<Pair<String, BaseRoute>>,
                       logoAction: HTMXAction,
                       profileMenuItems: List<ProfileMenuItem>,
                       current: BaseRoute) =
    Navbar(classes, user, navigations, logoAction, profileMenuItems, current, consumer).visit {
        render()
    }

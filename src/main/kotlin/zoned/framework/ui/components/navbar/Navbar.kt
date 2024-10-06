package zoned.framework.ui.components.navbar

import kotlinx.html.*
import zoned.framework.api.BaseRoute
import zoned.framework.auth.Identity
import zoned.framework.ui.components.buttons.HTMXAction
import zoned.framework.ui.components.html

class Navbar(val classes: String,
             private val identity: Identity,
             private val navigations: List<Pair<String, BaseRoute>>,
             private val profileMenuItems: List<ProfileMenuItem>,
             private val current: BaseRoute,
             private val logoAction: HTMXAction,
             private val logoText: String,
             private val logoImage: html.() -> Unit,
             consumer : TagConsumer<*>):
    NAV(mapOf("class" to "w-screen bg-white border-gray-200 dark:bg-gray-900 $classes"), consumer) {

    fun render() {
        div("flex flex-wrap items-center justify-between mx-auto p-4") {
            with (this@Navbar) {
                logo(logoAction, logoText, logoImage)

                mobileMenu("navbar-default")
                div("hidden w-full md:block md:w-auto") {
                    id = "navbar-default"
                    ul(
                        "font-medium w-full flex flex-col p-4 md:p-0 mt-4 border border-gray-100 rounded-lg bg-gray-50 " +
                        "md:flex-row md:space-x-8 md:mt-0 md:border-0 md:bg-white dark:bg-gray-900 " +
                        "dark:border-gray-700"
                    ) {
                        this@Navbar.navigations.forEach {
                            navbarLink(it.first, it.second, it.second == this@Navbar.current)
                        }
                    }
                }
                profileMenu(identity, profileMenuItems)
            }
        }
    }
}

// TODO maybe logometadata data class
fun FlowContent.navbar(classes: String,
                       user: Identity,
                       navigations: List<Pair<String, BaseRoute>>,
                       profileMenuItems: List<ProfileMenuItem>,
                       current: BaseRoute,
                       logoAction: HTMXAction,
                       logoText: String,
                       logoImage: html.() -> Unit) =
    Navbar(classes, user, navigations, profileMenuItems, current, logoAction, logoText, logoImage, consumer).visit {
        render()
    }

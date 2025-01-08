package zoned.framework.ui.components.navbar

import kotlinx.html.*
import zoned.framework.auth.Identity
import zoned.framework.ui.components.buttons.HTMXAction
import zoned.framework.ui.libs.onClick

class ProfileMenu(val user: Identity, val items: List<ProfileMenuItem>, consumer: TagConsumer<*>): DIV(mapOf(), consumer) {

    fun render() {
        div("flex items-center md:order-2 space-x-3 md:space-x-0 rtl:space-x-reverse") {
            button(classes = "flex text-sm bg-gray-800 rounded-full md:me-0 focus:ring-4 focus:ring-gray-300 " +
                    "dark:focus:ring-gray-600") {
                type = ButtonType.button
                id = "user-menu-button"
                attributes["aria-expanded"] = "false"
                attributes["data-dropdown-toggle"] = "user-dropdown"
                attributes["data-dropdown-placement"] = "bottom"
                // TODO sr-only isn't working
//                span("sr-only") { +"""Open user menu""" }
                img(classes = "w-8 h-8 rounded-full") {
                    src = "/static/profile-picture-3.jpg"
                    alt = "user photo"
                }
            }

            div("z-50 hidden my-4 text-base list-none bg-white divide-y divide-gray-100 rounded-lg shadow " +
                    "dark:bg-gray-700 dark:divide-gray-600") {
                id = "user-dropdown"
                div("px-4 py-3") {
                    span("block text-sm  text-gray-500 truncate dark:text-gray-400") { +this@ProfileMenu.user.email }
                }
                ul("py-2") {
                    attributes["aria-labelledby"] = "user-menu-button"

                    this@ProfileMenu.items.forEach { item ->
                        li {
                            a(classes = "block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600 " +
                                    "dark:text-gray-200 dark:hover:text-white") {
                                onClick(item.action)
                                href = "#"
                                +item.label
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ProfileMenuItem(val label: String, val action: HTMXAction)

fun Navbar.profileMenu(user: Identity, items: List<ProfileMenuItem>) {
    ProfileMenu(user, items, consumer).visit {
        render()
    }
}
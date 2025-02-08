package zoned.framework.ui.components.inputs

import kotlinx.html.FORM
import zoned.framework.api.BaseRoute
import zoned.framework.api.Method
import zoned.framework.api.Response
import zoned.framework.api.route
import kotlin.reflect.KFunction

fun FORM.action(route: BaseRoute) {
    val trigger = when (route.method) {
        Method.POST -> "hx-post"
        Method.GET -> "hx-get"
        Method.PUT -> "hx-put"
        Method.DELETE -> "hx-delete"
        Method.PATCH -> "hx-patch"
    }
    attributes[trigger] = route.path
}

fun FORM.action(handler: KFunction<Response>) {
    val route = route(handler)
    action(route)
}
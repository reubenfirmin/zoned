package zoned.framework.libs

import web.cssom.ClassName
import web.dom.document
import web.html.HTMLElement

object FlowbiteHelpers {

    fun clearOpenElements() {
        // this should work, but for some reason data-popper-placement is absent when we look at it
        val elements = document.querySelectorAll("[data-popper-placement]")
        for (i in 0 until elements.length) {
            val el = elements[i]
            (el as HTMLElement).classList.add(ClassName("hidden"))
        }
    }
}

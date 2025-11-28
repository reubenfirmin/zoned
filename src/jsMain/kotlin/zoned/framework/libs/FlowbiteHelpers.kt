package zoned.framework.libs

import kotlinx.browser.document
import org.w3c.dom.HTMLElement

object FlowbiteHelpers {

    fun clearOpenElements() {
        // this should work, but for some reason data-popper-placement is absent when we look at it
        val elements = document.querySelectorAll("[data-popper-placement]")
        for (i in 0 until elements.length) {
            val el = elements.item(i)!!
            (el as HTMLElement).classList.add("hidden")
        }
    }
}

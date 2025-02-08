package zoned.framework.routing

import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.id
import web.dom.document

data class Zone(val elementId: String) {

    companion object {

        fun id(zone: Zone) = document.getElementById(zone.elementId)

        fun CommonAttributeGroupFacade.zone(zone: Zone) {
            this.id = zone.elementId
        }
    }
}



import kotlinx.html.*
import kotlinx.html.stream.createHTML
import zoned.framework.ui.enhancements.SortableConfig
import zoned.framework.ui.enhancements.sortable

fun main() {
    val html = createHTML().html {
        head {
            title { +"Sortable Demo" }
            script { src = "/static/zoned.bundle.js" }
        }
        body {
            h1 { +"Property List - Drag to Reorder" }

            ul {
                id = "property-list"
                sortable(SortableConfig(
                    group = "properties",
                    animation = 200,
                    ghostClass = "opacity-50",
                    onEndUrl = "/api/properties/reorder"
                ))

                li {
                    attributes["data-property-id"] = "1"
                    +"123 Main St - $450,000"
                }
                li {
                    attributes["data-property-id"] = "2"
                    +"456 Oak Ave - $320,000"
                }
                li {
                    attributes["data-property-id"] = "3"
                    +"789 Pine Rd - $580,000"
                }
            }
        }
    }

    println("=== Generated HTML ===")
    println(html)
}

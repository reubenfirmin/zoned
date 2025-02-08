package zoned.framework.ui.components.table

import kotlinx.html.*
import zoned.framework.api.BaseRoute
import zoned.framework.ui.components.html
import zoned.framework.ui.components.table.Datatable.Companion.renderRows
import zoned.framework.ui.layouts.Fragment.zone
import zoned.framework.ui.layouts.HTMXTarget
import zoned.framework.ui.libs.HTMX
import zoned.framework.ui.libs.HTMX.htmxOnEvent

class Datatable<T, R>(classes: String,
                      val records: List<TableRecord<T, R>>,
                      val configs: List<FieldConfiguration<T, R>>,
                      val headingStyles: List<FieldHeadingConfiguration>? = null,
                      val rowStyle: String,
                      val autoScrollConfig: AutoScrollConfig?,
                      val emptyBlock: html.() -> Unit,
                      consumer: TagConsumer<*>):

    DIV(mapOf("class" to "w-full $classes"), consumer) {

    fun render() {

        table("w-full text-left border-spacing-2") {
            require(this@Datatable.configs.sumOf { it.width } == 12) {
                "Column sizes should total to 12 - ${this@Datatable.configs.joinToString(",") { it.width.toString() }}"
            }

            if (this@Datatable.headingStyles == null) {
                thead("text-xs text-gray-700 uppercase bg-gray-50 dark:bg-gray-700 dark:text-gray-400 sticky top-0") {
                    tr {
                        this@Datatable.configs.forEach { config ->
                            th(classes = "py-3 pl-2 w-${config.width}/12 pl-2") {
                                scope = ThScope.col
                                +config.field.title
                            }
                        }
                    }
                }
            } else {
                thead("sticky top-0") {
                    tr {
                        val configsByField = this@Datatable.configs.associateBy { it.field }
                        this@Datatable.headingStyles.forEach {
                            val width = configsByField[it.field]!!.width
                            it.renderer(this, width)
                        }
                    }
                }
            }

            // XXX cannot use tbody because it's not FlowContent - kotlinx.html bug
            if (this@Datatable.records.isNotEmpty()) {
                datatableTbody("") {
                    zone(tableTarget)
                    with(this@Datatable) {
                        renderRows(records, rowStyle, configs, autoScrollConfig)
                    }
                }
            }
        }

        if (records.isEmpty()) {
            div("flex justify-center pt-4 text-lg") {
                with (this@Datatable) {
                    emptyBlock()
                }
            }
        }
    }

    companion object {
        private val tableTarget = HTMXTarget("datatable")

        fun <T, R> FlowContent.renderRows(records: List<TableRecord<T, R>>, rowStyle: String, configs: List<FieldConfiguration<T, R>>, autoScrollConfig: AutoScrollConfig?) {
            val additionalCellStyle = if (rowStyle != DEFAULT_ROW_STYLE) {
                rowStyle
            } else {
                "pt-3 pb-3"
            }

            val fullPage = records.size >= (autoScrollConfig?.expectedRows ?: 0)

            records.forEachIndexed { idx, record ->
                tr(rowStyle) {
                    if (fullPage && autoScrollConfig != null && idx == (autoScrollConfig.expectedRows - 5)) {
                        // XXX it's hacky to assume these exist
                        val route = autoScrollConfig.loadNextPage
                            .params(
                                mapOf(
                                    "page" to (autoScrollConfig.page + 1),
                                    "target" to tableTarget.id,
                                    "limit" to autoScrollConfig.expectedRows))
                        // TODO make a handler for this
                        htmxOnEvent("revealed", route.url(), method = route.method, swap = HTMX.Swap.BEFOREEND)
                    }
                    // for each field, render
                    configs.forEach { config ->
                        // TODO this is a hack - we can't add margin/padding to tr
                        td("w-${config.width}/12 pl-2 " + additionalCellStyle) {
                            config.renderer(this@td, record)
                        }
                    }
                }
            }
        }

        // XXX this is due to a limitation of kotlink.html
        @HtmlTagMarker
        inline fun FlowContent.tr(classes : String? = null, crossinline block : TR.() -> Unit = {}) : Unit =
            TR(attributesMapOf("class", classes), consumer).visit(block)
    }
}

class DataTableTBody(initialAttributes : Map<String, String>, override val consumer : TagConsumer<*>):
    HTMLTag("tbody", consumer, initialAttributes, null, false, false), CommonAttributeGroupFacade, FlowContent

fun FlowContent.datatableTbody(classes: String, block: DataTableTBody.() -> Unit) {
    DataTableTBody(mapOf("class" to classes), consumer).visit {
        block()
    }
}

/**
 * @param loadNextPage must take page and target params, and must also add back the next load trigger (TODO messy)
 */
fun <T, R> FlowContent.datatable(classes: String,
                                 records: List<TableRecord<T, R>>,
                                 configs: List<FieldConfiguration<T, R>>,
                                 headingRenderers: List<FieldHeadingConfiguration>? = null,
                                 autoScrollConfig: AutoScrollConfig? = null,
                                 rowsOnly: Boolean = false,
                                 rowStyle: String = DEFAULT_ROW_STYLE,
                                 emptyBlock: html.() -> Unit) {
    if (rowsOnly) {
        renderRows(records, rowStyle, configs, autoScrollConfig)
    } else {
        Datatable(classes, records, configs, headingRenderers, rowStyle, autoScrollConfig, emptyBlock, consumer).visit {
            render()
        }
    }
}

const val DEFAULT_ROW_STYLE = "bg-white border-b dark:bg-gray-800 dark:border-gray-700 pt-2 hover:bg-gray-50 dark:hover:bg-gray-600"

/**
 * Route must take query params: target, page, limit
 */
data class AutoScrollConfig(val loadNextPage: BaseRoute, val expectedRows: Int, val page: Int)

data class Field(val title: String)

data class TableRecord<T, R>(val data: T, val metadata: R)

data class FieldConfiguration<T, R>(
    val field: Field,
    val visible: Boolean,
    val width: Int,
    val renderer: TD.(TableRecord<T, R>) -> Unit)

data class FieldHeadingConfiguration (
    val field: Field,
    val renderer: TR.(Int) -> Unit
)
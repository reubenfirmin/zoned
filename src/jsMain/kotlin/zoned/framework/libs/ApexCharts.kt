package zoned.framework.libs

import js.objects.unsafeJso
import web.dom.Element
import web.dom.NodeList
import web.dom.document
import web.html.HTMLElement
import zoned.framework.charts.*

@JsModule("apexcharts")
@JsNonModule
@JsName("ApexCharts")
external class ApexCharts(element: Element, options: dynamic) {
    fun render()
    fun updateOptions(options: dynamic)
}

fun initCharts() {
    val areaCharts = document.querySelectorAll("""[c-role="area-chart"]""")
    initCharts(areaCharts, ::initAreaChart)
    val barCharts = document.querySelectorAll("""[c-role="horizontal-bar-chart"]""")
    initCharts(barCharts, ::initBarChart)
}

fun initCharts(elements: NodeList<Element>, initializer: (HTMLElement) -> Unit) {
    for (i in 0 until elements.length) {
        val element = elements[i]!! as HTMLElement
        if (!element.hasAttribute("rendered-chart")) {
            initializer(element)
            element.setAttribute("rendered-chart", "true")
        }
    }
}

// TODO this stuff should go into a helpers
fun initAreaChart(element: HTMLElement) {
    val series = element.querySelectorAll("""[c-role="chart-series"]""").let { elements ->
        (0 until elements.length).mapNotNull {
            val el = elements[it]!! as HTMLElement
            val data = el.getAttribute("c-data")?.split(",")?.map { item ->
                item.trim().toDoubleOrNull()
            }?.toTypedArray<Number?>()
            val label = el.getAttribute("c-label")
            val color = el.getAttribute("c-color")
            val opposite = el.getAttribute("c-opposite") == "true"
            val min = el.getAttribute("c-min")?.toDouble() ?: 0.0
            val max = el.getAttribute("c-max")?.toDouble()
            if (data == null || label == null || color == null) {
                console.warn("Chart series must have c-data, c-label, c-color")
                null
            } else {
                val series: Series = unsafeJso {
                    this.data = data
                    this.name = label
                    this.color = color
                }
                series to AxisOptions(min = min, max = max, axis = if (opposite) {
                    Axis.RIGHT
                } else {
                    Axis.LEFT
                })
            }
        }
    }

    val xaxisLabels = element.querySelectorAll("""[c-role="chart-labels"]""").let { elements ->
        if (elements.length != 1)  {
            console.warn("Must be (only) one child with rcp_labels class")
            null
        } else {
            elements[0]?.let {
                (it as HTMLElement).getAttribute("c-data")?.split(",")?.map { item ->
                    item.trim()
                }?.toTypedArray()
            }
        }
    }

    if (series.isEmpty() || xaxisLabels == null) {
        console.warn("Must supply series for area charts")
    } else {
        AreaChart(element, series, xaxisLabels)
    }
}

fun initBarChart(element: HTMLElement) {
    val series: List<BarSeries<Double>> = element.querySelectorAll("""[c-role="chart-series"]""").let { elements ->
        (0 until elements.length).mapNotNull {
            val el = elements[it]!! as HTMLElement
            val data = el.getAttribute("c-data")?.split(",")?.map { item ->
                // XXX this is brittle
                item.trim().toDoubleOrNull()
            }?.filterNotNull()?:listOf()

            unsafeJso {
                this.name = el.getAttribute("c-name") ?: ""
                this.data = data.toTypedArray()
            }
        }
    }

    val labelsInBars = element.getAttribute("c-labels-in-bars") == "true"
    val labels = element.getAttribute("c-labels")?.split(",")?:listOf()

    if (series.isEmpty()) {
        console.warn("Must supply series for bar charts")
    } else {
        HorizontalBarChart(element, series.toTypedArray(), labels.toTypedArray(), labelsInBars) { value, context ->
            // TODO allow this to be passed in with attributes!
            "${context.w.config.xaxis.categories[context.dataPointIndex]} - $value%"
        }
    }
}

package zoned.framework.ui.components.charts

import kotlinx.html.*

data class Series<T: Number>(val data: List<T?>,
                             val label: String,
                             val color: String,
                             val min: Double? = 0.0,
                             val max: Double? = data.mapNotNull{ it?.toDouble() }.maxOfOrNull { it },
                             val opposite: Boolean = false)

class AreaChart<T: Number>(private val series: List<Series<T>>,
                           private val labels: List<String>,
                           private val height: Int,
                           private val ticks: Int, consumer: TagConsumer<*>): DIV(mapOf("class" to ""), consumer) {

    fun render() {
        attributes["c-role"] = "area-chart"
        attributes["c-height"] = height.toString()
        attributes["c-ticks"] = ticks.toString()
        series.forEach { serie ->
            span {
                attributes["c-role"] = "chart-series"
                attributes["c-data"] = serie.data.joinToString(",")
                attributes["c-label"] = serie.label
                attributes["c-color"] = serie.color
                attributes["c-opposite"] = serie.opposite.toString()
                attributes["c-min"] = serie.min.toString()
                if (serie.max != null) {
                    attributes["c-max"] = serie.max.toString()
                }
            }
        }
        span {
            attributes["c-role"] = "chart-labels"
            attributes["c-data"] = this@AreaChart.labels.joinToString(",")
        }
    }
}

fun <T: Number> FlowContent.areaChart(series: List<Series<T>>, labels: List<String>, height: Int = 360, ticks: Int = 6) = AreaChart(series, labels, height, ticks, consumer).visit {
    render()
}
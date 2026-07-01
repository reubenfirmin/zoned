package zoned.framework.ui.components.charts

import kotlinx.html.*

// TODO support multiple series (supported by frontend already)
class HorizontalBarChart(private val series: List<Double>,
                         private val labels: List<String>,
                         private val name: String,
                         private val labelsInBars: Boolean,
                         private val height: Int,
                         consumer: TagConsumer<*>): DIV(mapOf("class" to ""), consumer) {

    fun render() {
        attributes["c-role"] = "horizontal-bar-chart"
        attributes["c-labels"] = this@HorizontalBarChart.labels.joinToString(",")
        attributes["c-labels-in-bars"] = this@HorizontalBarChart.labelsInBars.toString()
        attributes["c-height"] = height.toString()

        span {
            attributes["c-role"] = "chart-series"

            if (this@HorizontalBarChart.series.isNotEmpty()) {
                // TODO this is not quite right - each series should have its own name...but we only have one series for now so future
                attributes["c-name"] = this@HorizontalBarChart.name
                attributes["c-data"] = this@HorizontalBarChart.series.joinToString(",")
            }
        }
    }
}

fun FlowContent.horizontalBarChart(series: List<Double>, labels: List<String>, name: String, labelsInBars: Boolean, height: Int = 320) =
    HorizontalBarChart(series, labels, name, labelsInBars, height, consumer).visit {
        render()
    }
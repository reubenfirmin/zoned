package zoned.framework.charts

import js.objects.unsafeJso
import web.cssom.ClassName
import web.dom.document
import web.dom.Element
import web.events.Event
import web.events.EventType
import web.events.addEventListener
import zoned.framework.libs.ApexCharts

external interface HorizontalBarOptions<Y: Number> {
    var chart: Chart
    var plotOptions: PlotOptions
    var series: Array<BarSeries<Y>>
    var dataLabels: DataLabels<Y>
    var tooltip: Tooltip
    var grid: Grid
    var responsive: Array<Responsive>
    var legend: Legend
    var xaxis: XAxis
    var yaxis: YAxis
}

external interface FormatterContext<Y: Number> {
    var seriesIndex: Int
    var dataPointIndex: Int
    var w: GlobalChartContext<Y>
}

external interface GlobalChartContext<Y: Number> {
    var config: HorizontalBarOptions<Y>
}

external interface DataLabels<Y: Number> {
    var enabled: Boolean
    var textAnchor: String
    var style: LabelStyle
    var formatter: (Y, FormatterContext<Y>) -> String
    var offsetX: Int
}

external interface PlotOptions {
    var bar: Orientation
}

external interface Orientation{
    var horizontal: Boolean
    var dataLabels: Position
}

external interface Position {
    var position: String
}

external interface BarSeries<Y: Number> {
    var name: String
    var data: Array<Y>
    var labels: Array<String>
}

class HorizontalBarChart<Y: Number>(element: Element,
                                    private val series: Array<BarSeries<Y>>,
                                    private val labels: Array<String>,
                                    private val labelsInBars: Boolean,
                                    private val formatter: (Y, FormatterContext<Y>) -> String) {

    init {
        val options = options(series, labelsInBars, formatter)
        console.dir(options)

        val chart = ApexCharts(element, options)
        chart.render()

        // init again when toggling dark mode
        document.addEventListener(EventType<Event>("dark-mode"), { _: Event ->
            chart.updateOptions(options(series, labelsInBars, formatter))
        })
    }

    // TODO much in common with the AreaChart. combine. also make more flexible
    private fun options(data: Array<BarSeries<Y>>, labelsInBars: Boolean, formatter: (Y, FormatterContext<Y>) -> String): HorizontalBarOptions<Y> {
        val darkMode = document.documentElement.classList.contains(ClassName("dark"))
        val labelColor = if (darkMode) "#9CA3AF" else "#6B7280"
        val fontFamily = "Inter, sans-serif"
        val theme = if (darkMode) "dark" else "light"

        val options: HorizontalBarOptions<Y> = unsafeJso {
            chart = unsafeJso {
                type = "bar"
                height = 420
                this.fontFamily = fontFamily
                foreColor = labelColor
                toolBar = unsafeJso {
                    show = true
                }
            }
            plotOptions = unsafeJso {
                bar = unsafeJso {
                    horizontal = true
                    dataLabels = unsafeJso {
                        position = "bottom"
                    }
                }
            }
            series = data
            dataLabels = unsafeJso {
                enabled = labelsInBars
                textAnchor = "start"
                style = unsafeJso {
                    this.fontFamily = fontFamily
                    this.fontSize = "18px"
                }
                offsetX = 0
                this.formatter = formatter
            }
            tooltip = unsafeJso {
                enabled = false
                style = unsafeJso {
                    this.fontFamily = fontFamily
                    fontSize = "14px"
                }
                this.theme = theme
            }
            grid = unsafeJso {
                show = true
                this.borderColor = borderColor
                strokeDashArray = 1
                padding = unsafeJso {
                    left = 35
                    bottom = 15
                }
            }
            yaxis = unsafeJso {
                show = false
            }
//            legend = jso {
//                fontSize = "14px"
//                fontWeight = 500
//                this.fontFamily = fontFamily
//                labels = jso {
//                    colors = arrayOf(labelColor)
//                }
//                itemMargin = jso {
//                    horizontal = 10
//                }
//            }
            responsive = arrayOf(unsafeJso {
                breakpoint = 1024
                options = unsafeJso {
                    xaxis = unsafeJso {
                        labels = unsafeJso {
                            show = false
                        }
                    }
                }
            })
            xaxis = unsafeJso {
                categories = this@HorizontalBarChart.labels
            }
        }
        return options
    }

}
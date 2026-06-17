package zoned.framework.charts

import js.objects.unsafeJso
import web.cssom.ClassName
import web.dom.document
import web.dom.Element
import web.events.Event
import web.events.EventType
import web.events.addEventListener
import zoned.framework.libs.ApexCharts

external interface ChartOptions {
    var chart: Chart
    var fill: Fill
    var dataLabels: Enabled
    var tooltip: Tooltip
    var grid: Grid
    var series: Array<Series>
    var markers: Markers
    var xaxis: XAxis
    var yaxis: Array<YAxis>
    var legend: Legend
    var responsive: Array<Responsive>
}

external interface Chart {
    var height: Int?
    var type: String
    var fontFamily: String?
    var foreColor: String?
    var toolBar: Showable?
}

external interface Gradient {
    var enabled: Boolean
    var opacityFrom: Double
    var opacityTo: Double
}

external interface Fill {
    var type: String
    var gradient: Gradient
}

external interface Enabled {
    var enabled: Boolean
}

external interface FontStyle {
    var fontSize: String
    var fontFamily: String
}

external interface Tooltip {
    var style: FontStyle
    var theme: String
    var enabled: Boolean
}

external interface Padding {
    var left: Int
    var bottom: Int
}

external interface Grid {
    var show: Boolean
    var borderColor: String
    var strokeDashArray: Int
    var padding: Padding
}


external interface Series {
    var name: String
    var data: Array<Number?>
    var color: String
}


external interface Hover {
    var size: String
    var sizeOffset: Int
}

external interface Markers {
    var size: Int
    var strokeColors: String
    var hover: Hover
}

external interface LabelStyle {
    var colors: Array<String>
    var fontSize: String
    var fontFamily: String
    var fontWeight: Int
}

external interface Label {
    var style: LabelStyle
}

external interface BorderColor {
    var color: String
}

external interface CrosshairStyle {
    var color: String
    var width: Int
    var dashArray: Int
}

external interface CrossHairs {
    var show: Boolean
    var position: String
    var stroke: CrosshairStyle
}

external interface XAxis {
    var categories: Array<String>
    var labels: Label
    var axisBorder: BorderColor
    var axisTicks: BorderColor
    var crossHairs: CrossHairs
}

external interface YAxis {
    var labels: LabelStyle
    var opposite: Boolean
    var title: Title
    var min: Double
    var max: Double?
    var show: Boolean
}

external interface Title {
    var text: String
}

external interface LegendLabelStyle {
    var colors: Array<String>
}

external interface ItemMargin {
    var horizontal: Int
}

external interface Legend {
    var fontSize: String
    var fontWeight: Int
    var fontFamily: String
    var labels: LegendLabelStyle
    var itemMargin: ItemMargin
}

external interface Showable {
    var show: Boolean
}

external interface ShowLabels {
    var labels: Showable
}

external interface ResponsiveOptions {
    var xaxis: ShowLabels
}

external interface Responsive {
    var breakpoint: Int
    var options: ResponsiveOptions
}

enum class Axis {
    LEFT, RIGHT
}

data class AxisOptions(val min: Double, val max: Double?, val axis: Axis)

class AreaChart(element: Element, series: List<Pair<Series, AxisOptions>>, xaxisLabels: Array<String>) {

    init {
        val chart = ApexCharts(element, options(series, xaxisLabels))
        chart.render()

        // init again when toggling dark mode
        document.addEventListener(EventType<Event>("dark-mode"), { _: Event ->
            chart.updateOptions(options(series, xaxisLabels))
        })
    }

    fun options(series: List<Pair<Series, AxisOptions>>, xaxisLabels: Array<String>): ChartOptions {
        val darkMode = document.documentElement.classList.contains(ClassName("dark"))
        val borderColor = if (darkMode) "#374151" else "#F3F4F6"
        val labelColor = if (darkMode) "#9CA3AF" else "#6B7280"
        val opacityFrom = if (darkMode) 0.0 else 0.15
        val opacityTo = if (darkMode) 0.45 else 0.0
        val theme = if (darkMode) "dark" else "light"
        val fontFamily = "Inter, sans-serif"

        val options: ChartOptions = unsafeJso {
            chart = unsafeJso {
                height = 420
                type = "area"
                this.fontFamily = fontFamily
                foreColor = labelColor
                toolBar = unsafeJso {
                    show = true
                }
            }
            fill = unsafeJso {
                type = "gradient"
                gradient = unsafeJso {
                    enabled = true
                    this.opacityFrom = opacityFrom
                    this.opacityTo = opacityTo
                }
            }
            dataLabels = unsafeJso {
                enabled = false
            }
            tooltip = unsafeJso {
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
            this.series = series.map { it.first }.toTypedArray()
            markers = unsafeJso {
                size = 5
                strokeColors = "#ffffff"
                hover = unsafeJso {
                    size = "2"
                    sizeOffset = 3
                }
            }
            xaxis = unsafeJso {
                categories = xaxisLabels
                labels = unsafeJso {
                    style = unsafeJso {
                        colors = arrayOf(labelColor)
                        fontSize = "14px"
                        fontWeight = 500
                    }
                }
                axisBorder = unsafeJso { color = borderColor }
                axisTicks = unsafeJso { color = borderColor }
                crossHairs = unsafeJso {
                    show = true
                    position = "back"
                    stroke = unsafeJso {
                        color = borderColor
                        width = 1
                        dashArray = 10
                    }
                }
            }
            yaxis = if (series.map { it.second }.toSet().size == 1) {
                arrayOf(unsafeJso {
                    labels = unsafeJso {
                        colors = arrayOf(labelColor)
                        fontSize = "14px"
                        fontWeight = 500
                    }
                    min = series.first().second.min
                    max = series.first().second.max
                })
            } else {
                series.map {
                    unsafeJso<YAxis> {
                        opposite = (it.second.axis == Axis.RIGHT)
                        title = unsafeJso {
                            text = it.second.axis.toString()
                        }
                        min = it.second.min
                        max = it.second.max
                    }
                }.toTypedArray()
            }
            legend = unsafeJso {
                fontSize = "14px"
                fontWeight = 500
                this.fontFamily = fontFamily
                labels = unsafeJso {
                    colors = arrayOf(labelColor)
                }
                itemMargin = unsafeJso {
                    horizontal = 10
                }
            }
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
        }
        return options
    }
}


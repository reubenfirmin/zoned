package zoned.framework.ui.components

import kotlinx.html.*
import kotlinx.html.TagConsumer
import zoned.framework.ui.tags.svg

class Accordion(consumer: TagConsumer<*>): DIV(mapOf(), consumer) {

    fun render() {
        id = "accordion-collapse"
        attributes["data-accordion"] = "collapse"
        h2("text-xl") {
            id = "accordion-collapse-heading-1"
            button(classes = "flex items-center justify-between w-full p-5 font-medium rtl:text-right text-gray-500 border border-b-0 border-gray-200 rounded-t-xl focus:ring-4 focus:ring-gray-200 dark:focus:ring-gray-800 dark:border-gray-700 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 gap-3") {
                type = ButtonType.button
                attributes["data-accordion-target"] = "#accordion-collapse-body-1"
                attributes["aria-expanded"] = "true"
                attributes["aria-controls"] = "accordion-collapse-body-1"
                span { +"Script 1: Qualify Lead" }
                svg("w-3 h-3 rotate-180 shrink-0") {
                    attributes["data-accordion-icon"] = "true"
                    attributes["aria-hidden"] = "true"
                    fill = "none"
                    viewBox = "0 0 10 6"
                    path {
                        stroke = "currentColor"
                        attributes["stroke-linecap"] = "round"
                        attributes["stroke-linejoin"] = "round"
                        attributes["stroke-width"] = "2"
                        d = "M9 5 5 1 1 5"
                    }
                }
            }
        }

        div("hidden") {
            id = "accordion-collapse-body-1"
            attributes["aria-labelledby"] = "accordion-collapse-heading-1"
            div("p-5 border border-b-0 border-gray-200 dark:border-gray-700 dark:bg-gray-900") {
                div("flex items-center p-2") {
                    input(classes = "w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500 dark:focus:ring-blue-600 dark:ring-offset-gray-800 focus:ring-2 dark:bg-gray-700 dark:border-gray-600") {
                        id = "checkbox1"
                        type = InputType.checkBox
                        value = ""
                    }
                    label("ms-2 text-sm font-medium text-gray-900 dark:text-gray-300") {
                        htmlFor = "checkbox1"
                        +" Lead intends to sell house"
                    }
                }

                div("flex items-center p-2") {
                    input(classes = "w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500 dark:focus:ring-blue-600 dark:ring-offset-gray-800 focus:ring-2 dark:bg-gray-700 dark:border-gray-600") {
                        id = "checkbox2"
                        type = InputType.checkBox
                        value = ""
                    }
                    label("ms-2 text-sm font-medium text-gray-900 dark:text-gray-300") {
                        htmlFor = "checkbox2"
                        +" Lead is owner of house, or is acting legally on behalf of owner"
                    }
                }

                div("flex items-center p-2") {
                    input(classes = "w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500 dark:focus:ring-blue-600 dark:ring-offset-gray-800 focus:ring-2 dark:bg-gray-700 dark:border-gray-600") {
                        id = "checkbox3"
                        type = InputType.checkBox
                        value = ""
                    }
                    label("ms-2 text-sm font-medium text-gray-900 dark:text-gray-300") {
                        htmlFor = "checkbox3"
                        +" Lead is willing to sell direct"
                    }
                }
            }
        }
        h2("text-xl") {
            id = "accordion-collapse-heading-2"
            button(classes = "flex items-center justify-between w-full p-5 font-medium rtl:text-right text-gray-500 border border-b-0 border-gray-200 focus:ring-4 focus:ring-gray-200 dark:focus:ring-gray-800 dark:border-gray-700 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 gap-3") {
                type = ButtonType.button
                attributes["data-accordion-target"] = "#accordion-collapse-body-2"
                attributes["aria-expanded"] = "false"
                attributes["aria-controls"] = "accordion-collapse-body-2"
                span { +"""Script 2: Reactivate stale lead""" }
                svg("w-3 h-3 rotate-180 shrink-0") {
                    attributes["data-accordion-icon"] = "true"
                    attributes["aria-hidden"] = "true"
                    fill = "none"
                    viewBox = "0 0 10 6"
                    path {
                        stroke = "currentColor"
                        attributes["stroke-linecap"] = "round"
                        attributes["stroke-linejoin"] = "round"
                        attributes["stroke-width"] = "2"
                        d = "M9 5 5 1 1 5"
                    }
                }
            }
        }
        div("hidden") {
            id = "accordion-collapse-body-2"
            attributes["aria-labelledby"] = "accordion-collapse-heading-2"
            div("p-5 border border-b-0 border-gray-200 dark:border-gray-700") {
                p("mb-2 text-gray-500 dark:text-gray-400") { +"""Flowbite is first conceptualized and designed using the Figma software so everything you see in the library has a design equivalent in our Figma file.""" }
                p("text-gray-500 dark:text-gray-400") {
                    +"""Check out the"""
                    a(classes = "text-blue-600 dark:text-blue-500 hover:underline") {
                        href = "https://flowbite.com/figma/"
                        +"""Figma design system"""
                    }
                    +"""based on the utility classes from Tailwind CSS and components from Flowbite."""
                }
            }
        }
        h2("text-xl") {
            id = "accordion-collapse-heading-3"
            button(classes = "flex items-center justify-between w-full p-5 font-medium rtl:text-right text-gray-500 border border-gray-200 focus:ring-4 focus:ring-gray-200 dark:focus:ring-gray-800 dark:border-gray-700 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 gap-3") {
                type = ButtonType.button
                attributes["data-accordion-target"] = "#accordion-collapse-body-3"
                attributes["aria-expanded"] = "false"
                attributes["aria-controls"] = "accordion-collapse-body-3"
                span { +"Script 3: Concrete intent to sell" }
                svg("w-3 h-3 rotate-180 shrink-0") {
                    attributes["data-accordion-icon"] = "true"
                    attributes["aria-hidden"] = "true"
                    fill = "none"
                    viewBox = "0 0 10 6"
                    path {
                        stroke = "currentColor"
                        attributes["stroke-linecap"] = "round"
                        attributes["stroke-linejoin"] = "round"
                        attributes["stroke-width"] = "2"
                        d = "M9 5 5 1 1 5"
                    }
                }
            }
        }
        div("hidden") {
            id = "accordion-collapse-body-3"
            attributes["aria-labelledby"] = "accordion-collapse-heading-3"
            div("p-5 border border-t-0 border-gray-200 dark:border-gray-700") {
                p("mb-2 text-gray-500 dark:text-gray-400") { +"""The main difference is that the core components from Flowbite are open source under the MIT license, whereas Tailwind UI is a paid product. Another difference is that Flowbite relies on smaller and standalone components, whereas Tailwind UI offers sections of pages.""" }
                p("mb-2 text-gray-500 dark:text-gray-400") { +"""However, we actually recommend using both Flowbite, Flowbite Pro, and even Tailwind UI as there is no technical reason stopping you from using the best of two worlds.""" }
                p("mb-2 text-gray-500 dark:text-gray-400") { +"""Learn more about these technologies:""" }
                ul("ps-5 text-gray-500 list-disc dark:text-gray-400") {
                    li {
                        a(classes = "text-blue-600 dark:text-blue-500 hover:underline") {
                            href = "https://flowbite.com/pro/"
                            +"""Flowbite Pro"""
                        }
                    }
                    li {
                        a(classes = "text-blue-600 dark:text-blue-500 hover:underline") {
                            href = "https://tailwindui.com/"
                            rel = "nofollow"
                            +"""Tailwind UI"""
                        }
                    }
                }
            }
        }
    }
}

fun FlowContent.accordion() {
    Accordion(consumer).visit {
        render()
    }
}
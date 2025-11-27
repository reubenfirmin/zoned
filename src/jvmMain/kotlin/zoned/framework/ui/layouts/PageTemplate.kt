package zoned.framework.ui.layouts

import io.javalin.http.Context
import kotlinx.html.FlowContent
import kotlinx.html.HEAD
import zoned.framework.ui.components.html

interface PageTemplate {

    fun HEAD.header()

    fun htmlClasses(): List<String> = emptyList()

    fun bodyClasses(): List<String>

    fun bodyContent(ctx: Context, html: html, blocks: List<html.() -> Unit>)

    fun slots(): Int

    fun slot(flowContent: html, block: html.() -> Unit) {
        with(flowContent) {
            block()
        }
    }
}
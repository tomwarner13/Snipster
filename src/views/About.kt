package com.okta.demo.ktor.views

import io.ktor.html.*
import kotlinx.html.FlowContent
import kotlinx.html.div

class About : Template<FlowContent> {
    override fun FlowContent.apply() {
        div("container px-0") {
        }

    }
}

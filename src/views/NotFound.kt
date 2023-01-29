package snipster.views

import io.ktor.html.*
import kotlinx.html.*

class NotFound(private val path: String) : Template<FlowContent> {
    override fun FlowContent.apply() {
        div("container px-0") {
            div("row justify-content-center") {
                div("col col-lg-8") {
                    h1 { +"Four, Oh Four!" }
                    p {
                        +"The page at '$path' does not exist, sorry. If this is a persistent or unexpected issue, you can "
                        a("https://github.com/tomwarner13/Snipster/issues/new") { +"file a bug report" }
                        +" on Github."
                    }
                }
            }
        }
    }
}
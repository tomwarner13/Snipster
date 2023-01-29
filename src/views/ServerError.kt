package snipster.views

import io.ktor.html.*
import io.ktor.http.*
import kotlinx.html.*

class ServerError(private val path: String, private val status: HttpStatusCode) : Template<FlowContent> {
    override fun FlowContent.apply() {
        div("container px-0") {
            div("row justify-content-center") {
                div("col col-lg-8") {
                    h1 { +"Server Error!" }
                    p {
                        +"The page at '$path' threw an internal server error with code ${status.value} and description '${status.description}', sorry. If this is a persistent or unexpected issue, you can "
                        a("https://github.com/tomwarner13/Snipster/issues/new") { +"file a bug report" }
                        +" on Github."
                    }
                }
            }
        }
    }
}
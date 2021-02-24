package com.okta.demo.ktor

import io.ktor.html.*
import kotlinx.html.*
import kotlinx.html.FormEncType.applicationXWwwFormUrlEncoded
import kotlinx.html.FormMethod.post
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class ScratchTemplate(private val currentUsername: String? = null) : Template<HTML> {
    val content = Placeholder<HtmlBlockTag>()
    override fun HTML.apply() {
        head {
            title { +"Snipster" }
            styleLink("https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css")
            script(src = "https://unpkg.com/codeflask/build/codeflask.min.js") {} //TODO import jquery here and alert script, add alerts
            script(src = "/scratchpad.js") {}
            meta(name = "viewport", content = "width=device-width, initial-scale=1, shrink-to-fit=no")
            meta(charset = "utf-8")
            style { unsafe { raw(".codeflask { max-height: 750px }") } } //TODO move to common stylesheet?
        }
        body("d-flex flex-column h-100") {
            header {
                div("navbar navbar-dark bg-dark shadow-sm") {
                    div("container") {
                        a(href = "/", classes = "font-weight-bold navbar-brand") {
                            +"📝 Snipster"
                        }
                        div("navbar-nav flex-row") {
                            if (currentUsername != null) {
                                a(href = "/${currentUsername}", classes = "nav-link mr-4") {
                                    +"Hello, $currentUsername"
                                }
                                a(href = "/logout", classes = "nav-link") {
                                    +"Logout"
                                }
                            } else {
                                div("navbar-text mr-4") {
                                    +"Hello, Guest"
                                }
                                div("navbar-item") {
                                    a(href = "/login", classes = "nav-link") {
                                        +"Login"
                                    }
                                }
                            }
                        }

                    }
                }
            }
            main("mt-3") {
                div("container") {
                    insert(content)
                }
            }
            script { //TODO: language selector dropdown?
                unsafe {
                    raw("""
                        //ur javascript goes here
                    """)
                }
            }
        }
    }
}

fun FlowContent.textEditor(document: String) {
    div {
        div { classes = setOf("codeflask", "col-lg-8", " col-xs-12", "border") }
    }
}

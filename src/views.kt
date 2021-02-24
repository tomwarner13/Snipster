package com.okta.demo.ktor

import io.ktor.html.*
import kotlinx.html.*
import kotlinx.html.FormEncType.applicationXWwwFormUrlEncoded
import kotlinx.html.FormMethod.post
import kotlinx.html.impl.DelegatingMap
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class ScratchTemplate(private val currentUsername: String? = null) : Template<HTML> {
    val content = Placeholder<HtmlBlockTag>()
    override fun HTML.apply() {
        head {
            title { +"Snipster" }
            styleLink("https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css")
            styleLink("/css/ohsnap.css")
            script(src = "https://unpkg.com/codeflask/build/codeflask.min.js") {}
            script(src = "https://code.jquery.com/jquery-3.5.1.min.js") {
                attributes["integrity"] = "sha256-9/aliU8dGd2tb6OSsuzixeV4y/faTqgFtohetphbbj0="
                attributes["crossorigin"] = "anonymous"
            }
            script(src = "/js/ohsnap.min.js") {}
            script(src = "/js/scratchpad.js") {}
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
                div { id="ohsnap" }
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

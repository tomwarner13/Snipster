package com.okta.demo.ktor

import io.ktor.html.*
import kotlinx.html.*
import kotlinx.html.FormEncType.applicationXWwwFormUrlEncoded
import kotlinx.html.FormMethod.post
import kotlinx.html.impl.DelegatingMap
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class ScratchTemplate(private val username: String? = null, private val displayname: String? = null) : Template<HTML> {
    val content = Placeholder<HtmlBlockTag>()
    val isLoggedIn = username !== null;
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
            script { //inject page-level variables
                unsafe {
                    raw("""
                        let isLoggedIn = $isLoggedIn;
                        let username = "$username";
                    """)
                }
            }
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
                            if (isLoggedIn) {
                                a(href = "/${username}", classes = "nav-link mr-4") { //TODO can this link to settings for the user? or just remove the <a>
                                    +"Hello, $displayname"
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

fun FlowContent.textEditor() {
    div {
        div { classes = setOf("codeflask", "col-lg-8", " col-xs-12", "border") }
    }
}

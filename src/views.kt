package com.okta.demo.ktor

import com.google.gson.Gson
import com.okta.demo.ktor.database.SnipRepository
import com.okta.demo.ktor.schema.Snip
import com.okta.demo.ktor.schema.SnipDc
import io.ktor.html.*
import kotlinx.html.*
import kotlinx.html.FormEncType.applicationXWwwFormUrlEncoded
import kotlinx.html.FormMethod.post
import kotlinx.html.impl.DelegatingMap
import org.kodein.di.LazyDI
import org.kodein.di.instance
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class ScratchTemplate(private val di: LazyDI, private val username: String? = null, private val displayname: String? = null) : Template<HTML> {
    private val repository by di.instance<SnipRepository>()
    private val isLoggedIn = username !== null;

    val content = Placeholder<HtmlBlockTag>()

    private val snips: Map<Int, SnipDc> = if (isLoggedIn) {
        username?.let { u -> repository.getSnipsByUser(u).map { it.id.value to it.toDc() }.toMap()}!!
    } else {
        emptyMap() //default snip here instead of server-side generate?
    }

    private val snipsJson = "let snips = " + Gson().toJson(snips) + ";"

    override fun HTML.apply() {
        head {
            title { +"Snipster" }
            styleLink("https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta2/dist/css/bootstrap.min.css")
            styleLink("/css/ohsnap.css")
            script(src = "https://unpkg.com/codeflask/build/codeflask.min.js") {}
            script(src = "https://code.jquery.com/jquery-3.5.1.min.js") {
                attributes["integrity"] = "sha256-9/aliU8dGd2tb6OSsuzixeV4y/faTqgFtohetphbbj0="
                attributes["crossorigin"] = "anonymous"
            }
            script(src = "https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta2/dist/js/bootstrap.bundle.min.js") {
                attributes["integrity"] = "sha384-b5kHyXgcpbZJO/tY9Ul7kGkf1S0CWuKcCD38l8YkeH8z8QjE0GmW1gYU5S9FOnJ0"
                attributes["crossorigin"] = "anonymous"
            }
            script(src = "/js/ohsnap.min.js") {}
            script { //inject page-level variables
                unsafe {
                    raw("""
                        let isLoggedIn = $isLoggedIn;
                        let username = "$username";
                        $snipsJson
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
                    ul("nav nav-tabs") {
                        var firstSnip = true
                        snips.forEach {
                            val activeClass = if (firstSnip) " active" else ""
                            li("nav-item") {
                                button(classes = "nav-link$activeClass", type = ButtonType.button) {
                                    id = "default-tab"
                                    attributes["data-bs-toggle"] = "tab"
                                    attributes["data-bs-target"] = "#"
                                    attributes["onclick"] = "loadActive(${it.key})"
                                    +it.value.title
                                }
                            }
                            firstSnip = false
                        }
                        if(firstSnip) { //snips were empty, use default title for first tab
                            li("nav-item") {
                                button(classes = "nav-link active", type = ButtonType.button) {
                                    id = "default-tab"
                                    attributes["data-bs-toggle"] = "tab"
                                    attributes["data-bs-target"] = "#"
                                    attributes["data-bs-target"] = "#"
                                    +"untitled"
                                }
                            }
                        }
                    }
                    insert(content)
                }
                div { id="ohsnap" }
            }
            /* script { //TODO: language selector dropdown? theme switcher?
                unsafe {
                    raw("//this is a script block")
                }
            } */
        }
    }
}

fun FlowContent.textEditor() {
    div {
        div { classes = setOf("codeflask", "col-lg-8", " col-xs-12", "border") }
    }
}

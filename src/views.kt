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
            styleLink("/fa/css/all.css")
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
            link(rel = "apple-touch-icon", href = "/apple-touch-icon.png") { sizes = "180x180" }
            link(rel="icon", type="image/png", href="/favicon-32x32.png") { sizes="32x32" }
            link(rel="icon", type="image/png", href="/favicon-16x16.png") { sizes="16x16" }
            link(rel="manifest", href="/site.webmanifest")
            link(rel="mask-icon", href="/safari-pinned-tab.svg") { attributes["color"]="#5bbad5" }
            meta(name="msapplication-TileColor", content = "#da532c")
            meta(name="theme-color", content = "#ffffff")
            style {
                unsafe {
                    raw(".codeflask { max-height: 750px }")
                    raw(".control-hidden { display: none }")
                }
            } //TODO move to common stylesheet?
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
                                a(href = "/${username}", classes = "nav-link mx-2") { //TODO can this link to settings for the user? or just remove the <a>
                                    +"Hello, $displayname"
                                }
                                a(href = "/logout", classes = "nav-link mx-2") {
                                    +"Logout"
                                }
                            } else {
                                div("navbar-text mx-2") {
                                    +"Hello, Guest"
                                }
                                div("navbar-item") {
                                    a(href = "/login", classes = "nav-link mx-2") {
                                        +"Login"
                                    }
                                }
                            }
                        }

                    }
                }
            }
            main("mt-3") {
                div("container px-0") {
                    ul("nav nav-tabs") {
                        var firstSnip = true
                        snips.forEach {
                            val activeClass = if (firstSnip) " active" else ""
                            li("nav-item") {
                                id = "snip-tab-${it.key}"
                                button(classes = "nav-link snip-tab-btn$activeClass", type = ButtonType.button) {
                                    id = "snip-btn-${it.key}"
                                    attributes["data-bs-toggle"] = "tab"
                                    attributes["data-bs-target"] = "#"
                                    attributes["onclick"] = "loadActive(${it.key})"
                                    i("fas fa-sticky-note me-1")
                                    span {
                                        id = "snip-name-${it.key}"
                                        +it.value.title
                                    }
                                }
                            }
                            firstSnip = false
                        }
                        if(firstSnip) { //snips were empty, use default title for first tab
                            li("nav-item") {
                                button(classes = "nav-link snip-tab-btn active", type = ButtonType.button) {
                                    id = "default-tab"
                                    attributes["data-bs-toggle"] = "tab"
                                    attributes["data-bs-target"] = "#"
                                    i("fas fa-sticky-note me-1")
                                    span {
                                        +"untitled"
                                    }
                                }
                            }
                        } else {
                            li("nav-item") {
                                id = "create-new-tab"
                                button(classes = "nav-link", type = ButtonType.button) {
                                    id = "create-new-btn"
                                    attributes["data-bs-toggle"] = "tab"
                                    attributes["data-bs-target"] = "#"
                                    onClick = "createNewSnip()"
                                    i("fas fa-plus")
                                }
                            }
                        }
                    }
                    nav("navbar border") {
                        div("nav") {
                            val disableClass = if (isLoggedIn) "" else " disabled"
                            val buttonClasses = "nav-link btn-outline mx-2 control-btn$disableClass"
                            val controlClasses = "$buttonClasses control-element"
                            val renameClasses = "$buttonClasses control-hidden rename-element"
                            val deleteClasses = "$buttonClasses control-hidden delete-element"
                            button(classes = controlClasses) {
                                id = "rename-snip-btn"
                                attributes["onclick"] = "renameDialog()"
                                +"Rename"
                                i("fas fa-edit")
                            }
                            button(classes = controlClasses) {
                                id = "delete-snip-btn"
                                attributes["onclick"] = "deleteDialog()"
                                +"Delete"
                                i("fas fa-trash")
                            }
                            input(InputType.text, classes = renameClasses) {
                                id = "rename-snip-input"
                            }
                            button(classes = "$renameClasses") {
                                id = "rename-snip-confirm-btn"
                                attributes["onclick"] = "renameSnip()"
                                +"Confirm"
                                i("fas fa-check")
                            }
                            button(classes = "$renameClasses") {
                                id = "rename-snip-cancel-btn"
                                attributes["onclick"] = "resetControls()"
                                +"Cancel"
                                i("fas fa-times")
                            }
                            span("$deleteClasses me-5") {
                                id = "delete-snip-confirm-message"
                                +"Really delete forever?"
                            }
                            button(classes = "$deleteClasses ms-5") {
                                id = "delete-snip-confirm-btn"
                                attributes["onclick"] = "deleteSnip()"
                                +"Confirm"
                                i("fas fa-check")
                            }
                            button(classes = deleteClasses) {
                                id = "delete-snip-cancel-btn"
                                attributes["onclick"] = "resetControls()"
                                +"Cancel"
                                i("fas fa-times")
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
    div("codeflask col-lg-8 col-xs-12 border container px-0")
}
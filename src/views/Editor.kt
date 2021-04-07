package com.okta.demo.ktor.views

import com.google.gson.Gson
import com.okta.demo.ktor.database.SnipRepository
import com.okta.demo.ktor.schema.SnipDc
import io.ktor.html.*
import kotlinx.html.*
import org.kodein.di.LazyDI
import org.kodein.di.instance

class Editor(private val snips: Map<Int, SnipDc>, username: String? = null) : Template<FlowContent> {
    private val isLoggedIn = username != null

    override fun FlowContent.apply() {
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
                    button(classes = renameClasses) {
                        id = "rename-snip-confirm-btn"
                        attributes["onclick"] = "renameSnip()"
                        +"Confirm"
                        i("fas fa-check")
                    }
                    button(classes = renameClasses) {
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
            div("codeflask col-lg-8 col-xs-12 border container px-0")
        }
    }

    companion object {
        fun getSnipsForUser(di: LazyDI, username: String?) : Map<Int, SnipDc> {
            if(username == null) return emptyMap()

            val repository by di.instance<SnipRepository>()
            return username.let { u -> repository.getSnipsByUser(u).map { it.id.value to it.toDc() }.toMap()}
        }
    }
}


fun HEAD.editorSpecificHeaders(snips: Map<Int, SnipDc>, username: String? = null) {
    val isLoggedIn = username != null

    val snipsJson = "let snips = " + Gson().toJson(snips) + ";"

    script(src = "https://unpkg.com/codeflask/build/codeflask.min.js") {}
    script(src = "/js/scratchpad.js") {}
    script {
        unsafe {
            raw("""
                        let isLoggedIn = $isLoggedIn;
                        let username = "$username";
                        $snipsJson
                    """)
        }
    }
    style {
        unsafe {
            raw(".codeflask { max-height: 750px }\n")
            raw(".control-hidden { display: none }\n")
        }
    } //TODO move to common stylesheet?
}
package snipster.views

import snipster.config.AppConfig
import io.ktor.html.*
import kotlinx.html.*

class PageTemplate(private val pageHeader: String, username: String? = null) : Template<HTML> {
    private val isLoggedIn = username != null

    val headerContent = Placeholder<HEAD>()
    val pageContent = Placeholder<FlowContent>()
    val navBarContent = Placeholder<FlowContent>()

    override fun HTML.apply() {
        head {
            title { +pageHeader }
            styleLink("https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta2/dist/css/bootstrap.min.css")
            styleLink("/fa/css/all.css")
            script(src = "https://code.jquery.com/jquery-3.5.1.min.js") {
                attributes["integrity"] = "sha256-9/aliU8dGd2tb6OSsuzixeV4y/faTqgFtohetphbbj0="
                attributes["crossorigin"] = "anonymous"
            }
            script(src = "https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta2/dist/js/bootstrap.bundle.min.js") {
                attributes["integrity"] = "sha384-b5kHyXgcpbZJO/tY9Ul7kGkf1S0CWuKcCD38l8YkeH8z8QjE0GmW1gYU5S9FOnJ0"
                attributes["crossorigin"] = "anonymous"
            }
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
                    raw(".dropdown-menu[data-bs-popper] { left: unset !important; }\n" + //fixes dumb bootstrap bug dragging dropdowns all the way to the left idk
                            ".connection-lost-container { display: none; padding: .5rem 1rem }\n" + //hides connection lost icon
                            ".codejar-linenumbers { z-index: 2 }" //prevents scrolled text from "hovering" above line numbers in editor
                    )
                }
            }
            insert(headerContent)
        }
        body("d-flex flex-column h-100") {
            header {
                div("navbar navbar-dark bg-dark shadow-sm") {
                    div("container") {
                        a(href = "/", classes = "font-weight-bold navbar-brand") {
                            +"📝 Snipster"
                        }
                        a("#", classes = "navbar-brand dropdown-toggle me-auto") {
                            attributes["data-bs-toggle"] = "dropdown"
                            role="button"
                        }
                        ul("dropdown-menu") {
                            li {
                                a("/about", classes="dropdown-item") {
                                    +"About"
                                }
                            }
                        }
                        insert(navBarContent)
                    }
                }
            }
            main("mt-3") {
                insert(pageContent)
            }
        }
    }
}
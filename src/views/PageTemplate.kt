package com.okta.demo.ktor.views

import com.okta.demo.ktor.config.AppConfig
import io.ktor.html.*
import kotlinx.html.*

class PageTemplate(private val appConfig: AppConfig, private val pageHeader: String, username: String? = null, private val displayName: String? = null) : Template<HTML> {
    private val isLoggedIn = username != null
    private val oktaConfig = appConfig.oktaConfig

    val headerContent = Placeholder<HEAD>()
    val pageContent = Placeholder<FlowContent>()

    override fun HTML.apply() {
        head {
            title { +pageHeader }
            styleLink("https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta2/dist/css/bootstrap.min.css")
            styleLink("/fa/css/all.css")
            if(!isLoggedIn) {
                styleLink("https://global.oktacdn.com/okta-signin-widget/5.5.1/css/okta-sign-in.min.css")
                script(src = "https://global.oktacdn.com/okta-signin-widget/5.5.1/js/okta-sign-in.min.js") {}
            }
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
                        div("navbar-nav flex-row") {
                            span("connection-lost-container") {
                                attributes["title"] = "Connection lost!"
                                i("fas fa-unlink text-danger")
                            }
                            if (isLoggedIn) {
                                a(classes = "nav-link mx-2") {
                                    attributes["data-bs-toggle"] = "modal"
                                    attributes["data-bs-target"] = "#settingsModal"
                                    i("fas fa-user-cog text-light me-1")
                                    +"Hello, $displayName"
                                }
                                a(href = "/logout", classes = "nav-link mx-2") {
                                    +"Logout"
                                }
                            } else {
                                div("navbar-text mx-2") {
                                    +"Hello, Guest"
                                }
                                div("navbar-item") {
                                    button(classes = "btn btn-secondary", type = ButtonType.button) {
                                        attributes["data-bs-toggle"] = "modal"
                                        attributes["data-bs-target"] = "#loginModal"
                                        +"Login"
                                    }
                                }
                            }
                        }
                    }
                }
            }
            main("mt-3") {
                insert(pageContent)
                if(isLoggedIn) {
                    div("modal fade") {
                        id = "settingsModal"
                        div("modal-dialog") {
                            div("modal-content") {
                                div("modal-header") {
                                    h5("modal-title") {
                                        i("fas fa-user-cog me-1")
                                        +"User Settings"
                                    }
                                    button(classes = "btn-close", type = ButtonType.button) {
                                        attributes["data-bs-dismiss"] = "modal"
                                    }
                                }
                                div("modal-body") {
                                    div {
                                        id = "userSettingsForm"
                                        //known settings so far:
                                        //  addClosing on ', default to off, bool
                                        //  line numbers vs word wrap, some kind of toggle
                                        //  anything else?

                                        //this will need:
                                        //  a DB table with username as key
                                        //  a fetch method to load the DB table
                                        //  some way to communicate with the table and push change events -- use the socket?
                                        //  theme and syntax highlighting eventually but that may not be at the user level
                                    }
                                }
                            }
                        }
                    }
                } else {
                    div("modal fade") {
                        id = "loginModal"
                        div("modal-dialog") {
                            div("modal-content") {
                                div("modal-header") {
                                    h5("modal-title") {
                                        +"Login or Sign Up"
                                    }
                                    button(classes = "btn-close", type = ButtonType.button) {
                                        attributes["data-bs-dismiss"] = "modal"
                                    }
                                }
                                div("modal-body") {
                                    div {
                                        id = "oktaLoginContainer"
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(!isLoggedIn) {
                script {
                    unsafe {
                        raw("""
                            let signIn = new OktaSignIn({
                                baseUrl: '${oktaConfig.oktaHost}',
                                el: '#oktaLoginContainer',
                                clientId: '${oktaConfig.clientId}',
                                redirectUri: '${appConfig.host}/login/authorization-callback',
                                authParams: {
                                  scopes: ['openid', 'email', 'profile'],
                                  issuer: '${oktaConfig.orgUrl}',
                                  pkce: false,
                                  responseType: 'code',
                                  nonce: null
                                },
                                features: {
                                  registration: true,
                                  rememberMe: true
                                },
                                i18n: {
                                  'en': {
                                    'primaryauth.username.placeholder': 'Email Address',
                                    'primaryauth.username.tooltip': 'Your email address is your username.'
                                  }
                                }
                              }
                            );
                            
                            signIn.showSignInAndRedirect()
                            .catch((e) => {
                              console.log("sign in error! " + e);
                            });
                        """.trimIndent())
                    }
                }
            }
        }
    }
}
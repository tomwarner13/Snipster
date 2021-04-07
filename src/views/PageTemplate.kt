package com.okta.demo.ktor.views

import com.okta.demo.ktor.oktaConfigReader
import com.typesafe.config.ConfigFactory
import io.ktor.html.*
import kotlinx.html.*
import java.lang.Exception

class PageTemplate(private val pageHeader: String, private val username: String? = null, private val displayName: String? = null) : Template<HTML> {
    private val isLoggedIn = username != null
    private val oktaConfig = oktaConfigReader(ConfigFactory.load() ?: throw Exception("Failed to load okta config"))

    val headerContent = Placeholder<HEAD>()
    val pageContent = Placeholder<FlowContent>()

    override fun HTML.apply() {
        head {
            title { +pageHeader }
            styleLink("https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta2/dist/css/bootstrap.min.css")
            styleLink("/css/ohsnap.css")
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
            script(src = "/js/ohsnap.min.js") {}
            meta(name = "viewport", content = "width=device-width, initial-scale=1, shrink-to-fit=no")
            meta(charset = "utf-8")
            link(rel = "apple-touch-icon", href = "/apple-touch-icon.png") { sizes = "180x180" }
            link(rel="icon", type="image/png", href="/favicon-32x32.png") { sizes="32x32" }
            link(rel="icon", type="image/png", href="/favicon-16x16.png") { sizes="16x16" }
            link(rel="manifest", href="/site.webmanifest")
            link(rel="mask-icon", href="/safari-pinned-tab.svg") { attributes["color"]="#5bbad5" }
            meta(name="msapplication-TileColor", content = "#da532c")
            meta(name="theme-color", content = "#ffffff")
            insert(headerContent)
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
                div { id="ohsnap" }
                if(!isLoggedIn) {
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
                                redirectUri: '${oktaConfig.host}/login/authorization-callback',
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
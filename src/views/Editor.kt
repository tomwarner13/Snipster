package snipster.views

import com.google.gson.Gson
import snipster.database.SnipRepository
import snipster.schema.SnipDc
import io.ktor.html.*
import kotlinx.html.*
import org.kodein.di.LazyDI
import org.kodein.di.instance
import snipster.config.AppConfig
import snipster.config.OktaConfig
import snipster.schema.UserSettingsDc

class Editor(private val snips: Map<Int, SnipDc>, private val appConfig: AppConfig, private val settings: UserSettingsDc, username: String? = null) : Template<FlowContent> {
    private val isLoggedIn = username != null
    private val oktaConfig = appConfig.oktaConfig

    override fun FlowContent.apply() {
        div {
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
                                    if(firstSnip) {
                                        a("#", classes = "dropdown-toggle mx-1 default-link") {
                                            attributes["data-bs-toggle"] = "dropdown"
                                            role="button"
                                        }
                                        ul("dropdown-menu") {
                                            li {
                                                a("/about", classes="dropdown-item") {
                                                    +"Test Control Button"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        firstSnip = false
                    }
                    if (firstSnip) { //snips were empty, use default title for first tab
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
                pre {
                    id = "editor"
                    code("col-lg-8 col-xs-12 container px-0 code language-markdown")
                }
            }
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
                                    p {
                                        input(InputType.checkBox, classes = "form-check-label") {
                                            id = "addClosingBox"
                                            checked = settings.insertClosing
                                        }
                                        label {
                                            attributes["for"] = "addClosingBox"
                                            +"Automatically add closing quotes when typing?"
                                        }
                                    }
                                    p {
                                        input(InputType.checkBox, classes = "form-check-label") {
                                            id = "useLineNumbersBox"
                                            checked = settings.useLineNumbers
                                        }
                                        label {
                                            attributes["for"] = "useLineNumbersBox"
                                            +"Use line numbers? (also disables word wrap)"
                                        }
                                    }
                                    p {
                                        button(classes = "nav-link btn-outline mx-2") {
                                            id = "save-user-settings-btn"
                                            attributes["onclick"] = "saveSettings()"
                                            +"Save"
                                            i("fas fa-check")
                                        }
                                    }
                                    //known settings so far:
                                    //  addClosing on ', default to off, bool
                                    //  line numbers vs word wrap, some kind of toggle
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
                                  rememberMe: true,
                                  showPasswordToggleOnSignInPage: true
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


fun HEAD.editorSpecificHeaders(snips: Map<Int, SnipDc>, settings: UserSettingsDc, username: String? = null) {
    val isLoggedIn = username != null

    val snipsJson = "let snips = ${Gson().toJson(snips)};"
    val settingsJson = "let settings = ${Gson().toJson(settings)};"

    val defaultContent = if (!isLoggedIn) """
    let defaultContent = "welcome!\n\nSnipster is a plaintext editor which supports multiple tabs (snips) and updates across all open browsers instantly. more info is available on the 'About' page, under the top menu.\n\nyou'll need to create an account to do that--it's free and does not require email verification. simply click 'Login' above and then 'Sign Up'\n\nyou can edit this document, and edits will persist in this browser until you clear your cache--if you want it to go back to default, click 'reset' above";
    """
    else "";

    script(type="module", src = "/js/prism.js") {}
    script(type="module", src = "/js/scratchpad.js") {}
    if(!isLoggedIn) {
        styleLink("https://global.oktacdn.com/okta-signin-widget/5.5.1/css/okta-sign-in.min.css")
        script(src = "https://global.oktacdn.com/okta-signin-widget/5.5.1/js/okta-sign-in.min.js") {}
    }
    script {
        unsafe {
            raw("""
                        let isLoggedIn = $isLoggedIn;
                        let username = "$username";
                        $defaultContent
                        $settingsJson
                        $snipsJson
                    """)
        }
    }
    styleLink("/css/prism.css")
    style {
        unsafe {
            raw(".control-hidden { display: none }\n" +
                    ".connection-lost-container { display: none; padding: .5rem 1rem }\n" + //hides connection lost icon
                    ".codejar-linenumbers { z-index: 2 }\n" + //prevents scrolled text from "hovering" above line numbers in editor
                    "#editor { min-height: 400px }\n" + //keeps the editor from shrinking to the size of the text when there's very little
                    ".default-link { color: inherit; text-decoration: inherit; }\n" + //these all prevent the lil action dropdown from
                    ".default-link:link { color: inherit; text-decoration: inherit; }\n" + //looking like a link (aka being blue)
                    ".default-link:hover { color: inherit; text-decoration: inherit; }\n"
            )
        }
    } //TODO move to common stylesheet?
}

class EditorSpecificNavbarTemplate(private val isLoggedIn: Boolean, private val displayName: String?) : Template<FlowContent> {
    override fun FlowContent.apply() {
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
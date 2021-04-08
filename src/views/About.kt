package com.okta.demo.ktor.views

import io.ktor.html.*
import kotlinx.html.*

class About : Template<FlowContent> {
    override fun FlowContent.apply() {
        div("container px-0") {
            div("row justify-content-center") {
                div("col col-lg-8") {
                    h2 { +"What is Snipster?" }
                    p { +"It's a notes app that doesn't hate you. It allows you to edit multiple note tabs (snips) on any device you're logged in on, without having to save or refresh anything -- updates are applied across all connected clients instantly. Think a single instance of your favorite tab-supporting plaintext editor running across as many browsers as you want." }
                    h2 { +"What doesn't it do yet?" }
                    p { +"Planned future features include collaboration (you'll be able to give multiple users read and/or edit permissions on a per-snip basis), tab reordering, and syntax highlighting. I also plan to develop native app clients for IOS and Android, though Snipster is and will always be supported in mobile browsers -- I'll never force or even bother you about app installs. There will eventually be a paid tier that allows for more open snips per user, a larger character limit per snip, and collaboration with more people." }
                    h2 { +"What won't it ever do?" }
                    p { +"There will never be any formatting of any kind applied to your text -- no bold, no italics, no munging your tabs because you pasted in some code and the editor thinks you wanted a 3-level nested bulleted list. The world has no need of any more word processors. While basic syntax highlighting will be supported in the future, this will never be a sophisticated code editor -- there are in-browser IDEs and code fiddle tools available if you need that. There will not be any sophisticated document history or change-tracking tools." }
                    h2 { +"Who makes it?" }
                    p {
                        +"Hi! I'm Tom. I'm the development team. I've previously worked for a few software companies you might have heard of, but right now I'm developing Snipster full time. You can reach out to me with bug reports, feature requests, etc on the "
                        a("https://github.com/tomwarner13/Snipster") { +"Github repo." }
                    }
                    h2 { +"Does it cost anything?" }
                    p { +"Right now, no. In the future, paid subscriptions will have access to more snips and collaboration with more people than free-tier users (free-tier will always be supported for casual personal use)." }
                    h2 { +"Can I pay you anyway?" }
                    p {
                        +"If you insist! My Venmo link is @Tom-Warner-1, you can PayPal me through the email posted on my "
                        a("https://github.com/tomwarner13") { +"Github" }
                        +", or you can email it directly for alternative arrangements (crypto etc)."
                    }
                }
            }
        }
    }
}

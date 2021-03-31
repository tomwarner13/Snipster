package com.okta.demo.ktor

import com.okta.jwt.JwtVerifiers
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import java.lang.Exception


fun Application.setupAuth() {
    val oktaConfig = oktaConfigReader(ConfigFactory.load() ?: throw Exception("Failed to load okta config"))

    install(Authentication) {
        oauth {
            urlProvider = { "${oktaConfig.host}/login/authorization-callback" }
            providerLookup = { oktaConfig.asOAuth2Config() }
            client = HttpClient()
        }
    }

    val accessTokenVerifier = JwtVerifiers.accessTokenVerifierBuilder()
        .setAudience(oktaConfig.audience)
        .setIssuer(oktaConfig.orgUrl)
        .build()

    val idVerifier = JwtVerifiers.idTokenVerifierBuilder()
        .setClientId(oktaConfig.clientId)
        .setIssuer(oktaConfig.orgUrl)
        .build()

    val host = oktaConfig.host

    routing {
        authenticate {
            get("/login/authorization-callback") {
                // get principle from token
                val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                    ?: throw Exception("no principle found (democrat detected)")

                //parse and verify token
                val accessToken = accessTokenVerifier.decode(principal.accessToken)

                //get id token, parse and verify
                val idTokenString = principal.extraParameters["id_token"]
                    ?: throw Exception("id token not found")
                val idToken = idVerifier.decode(idTokenString, null)

                //try to get handle, fallback to subject field
                val fullName = (idToken.claims["name"] ?: accessToken.claims["sub"] ?: "UNKNOWN_NAME").toString()
                val username = idToken.claims["preferred_username"].toString()
                log.debug("user $fullName : $username logged in")

                // Create a session object with "slugified" username
                val session = UserSession(
                    username = username,
                    displayName = fullName.split(" ").first(),
                    idToken = idTokenString
                )
                call.sessions.set(session)
                call.respondRedirect("/")
            }

            get("/login") {
                call.respondRedirect("/")
            }
        }

        get("/logout") {
            log.debug("entered logout code")
            val idToken = call.session?.idToken

            call.sessions.clear<UserSession>()

            val redirectLogout = when (idToken) {
                null -> "/"
                else -> URLBuilder(oktaConfig.logoutUrl).run {
                    parameters.append("post_logout_redirect_uri", host)
                    parameters.append("id_token_hint", idToken)
                    buildString()
                }
            }

            call.respondRedirect(redirectLogout)
        }
    }
}
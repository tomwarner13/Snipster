package com.okta.demo.ktor

import com.typesafe.config.Config
import io.ktor.auth.*
import io.ktor.config.*
import io.ktor.http.*

data class OktaConfig(
    val orgUrl: String,
    val clientId: String,
    val clientSecret: String,
    val audience: String,
    val host: String
) {
    val accessTokenUrl = "$orgUrl/v1/token"
    val authorizeUrl = "$orgUrl/v1/authorize"
    val logoutUrl = "$orgUrl/v1/logout"

    fun asOAuth2Config(): OAuthServerSettings.OAuth2ServerSettings =
        OAuthServerSettings.OAuth2ServerSettings(
            name = "okta",
            authorizeUrl = authorizeUrl,
            accessTokenUrl = accessTokenUrl,
            clientId = clientId,
            clientSecret = clientSecret,
            defaultScopes = listOf("openid", "profile"),
            requestMethod = HttpMethod.Post
        )
}

fun oktaConfigReader(config: Config): OktaConfig = OktaConfig(
    orgUrl = config.getString("okta.orgUrl"),
    clientId = config.getString("okta.clientId"),
    clientSecret = config.getString("okta.clientSecret"),
    audience = config.tryGetString("okta.audience") ?: "api://default",
    host = config.getString("global.host"),
)
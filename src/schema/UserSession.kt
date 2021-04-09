package com.okta.demo.ktor.schema

//add name values here? id by first name?
data class UserSession(
    val username: String,
    val displayName: String,
    val idToken: String
)
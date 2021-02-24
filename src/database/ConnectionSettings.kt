package com.okta.demo.ktor.database

data class ConnectionSettings(
    val url: String,
    val driver: String,
    val user: String,
    val password: String
)

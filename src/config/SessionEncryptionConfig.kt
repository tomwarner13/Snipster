package com.okta.demo.ktor.config

data class SessionEncryptionConfig(
    val encryptionKey: String,
    val authKey: String
)

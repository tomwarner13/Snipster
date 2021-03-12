package com.okta.demo.ktor.cache

interface CacheProvider {
    fun putObject(key: String, value: Any, expirationInSeconds: Int)

    fun <T> getOrFetchObject(key: String, loader: () -> T, expirationInSeconds: Int) : T
}
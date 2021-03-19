package com.okta.demo.ktor.cache

interface CacheProvider { //might need to be CacheProvider<T>?
    fun<T> putObject(key: String, value: T)

    fun<T> getOrFetchObject(key: String, loader: () -> T) : T

    fun<T> getIfExists(key: String) : T?
}
package com.okta.demo.ktor.cache

import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration

class MemoryCacheProvider : CacheProvider {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(5))
        .build<String, Any>()

    override fun <T : Any> putObject(key: String, value: T) {
        cache.put(key, value)
    }

    override fun <T : Any> getOrFetchObject(key: String, loader: () -> T): T {
        val result = cache.get(key) { loader() } as T

        if(result != null) return result

        //should never be null because loader() should set the object
        throw IllegalStateException("Unable to access cache object!")
    }

    override fun <T> getIfExists(key: String): T? {
        return cache.getIfPresent(key) as T
    }
}
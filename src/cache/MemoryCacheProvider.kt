package com.okta.demo.ktor.cache

import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration

class MemoryCacheProvider : CacheProvider {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(5))
        .build<String, Any>()

    override fun <T> putObject(key: String, value: T) {
        cache.put(key, value)
    }

    override fun <T> getOrFetchObject(key: String, loader: () -> T): T {
        val result = cache.get(key) { loader() } as T

        if(result != null) return result

        throw IllegalStateException("Unable to access cache object!")
    }

    override fun <T> getIfExists(key: String): T? {
        val result = cache.getIfPresent(key) as T

        if(result != null) return result

        throw IllegalStateException("Unable to access cache object!")
    }
}
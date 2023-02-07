package snipster.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CacheProvider { //might need to be CacheProvider<T>?
    fun<T : Any> putObject(key: String, value: T)

    fun<T : Any> getOrFetchObject(key: String, loader: () -> T) : T

    suspend fun <T : Any> getOrFetchObjectAsync(key: String, loader: () -> T): T

    fun<T> getIfExists(key: String) : T?
}
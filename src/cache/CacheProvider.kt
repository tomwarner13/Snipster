package snipster.cache

interface CacheProvider { //might need to be CacheProvider<T>?
    fun<T : Any> putObject(key: String, value: T)

    fun<T : Any> getOrFetchObject(key: String, loader: () -> T) : T

    fun<T> getIfExists(key: String) : T?
}
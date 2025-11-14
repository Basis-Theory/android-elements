package com.basistheory.elements.util

import com.basistheory.elements.model.BinDetails
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe cache for BIN details to avoid redundant API calls
 * Uses an in-memory cache with a maximum size limit
 */
internal object BinDetailsCache {
    private const val MAX_CACHE_SIZE = 100
    private val cache = ConcurrentHashMap<String, BinDetails>()
    
    /**
     * Retrieves cached bin details for the given BIN
     * @param bin The BIN (first 6 digits of card number)
     * @return Cached BinDetails or null if not found
     */
    fun get(bin: String): BinDetails? {
        return cache[bin]
    }
    
    /**
     * Stores bin details in the cache
     * @param bin The BIN (first 6 digits of card number)
     * @param details The BinDetails to cache
     */
    fun put(bin: String, details: BinDetails) {
        // Implement simple LRU by clearing oldest entries when cache is full
        if (cache.size >= MAX_CACHE_SIZE) {
            val oldestKey = cache.keys.firstOrNull()
            oldestKey?.let { cache.remove(it) }
        }
        cache[bin] = details
    }
    
    /**
     * Clears all cached bin details
     */
    fun clear() {
        cache.clear()
    }
}

package com.homeassistant.domain.port

import com.homeassistant.domain.model.EventHash
import com.homeassistant.domain.model.EventLog

/**
 * Port for filtering duplicate events
 * Implementation should handle the storage and TTL logic
 */
interface DuplicateFilterPort {
    /**
     * Checks if an event is a duplicate within the configured TTL
     * @param event The event to check
     * @return true if the event is a duplicate, false otherwise
     */
    fun isDuplicate(event: EventLog): Boolean

    /**
     * Records an event hash to prevent future duplicates
     * @param eventHash The hash to store
     */
    fun recordEvent(eventHash: EventHash)

    /**
     * Clears expired entries from the filter
     * This should be called periodically to prevent memory leaks
     */
    fun cleanup()

    /**
     * Gets the current count of stored event hashes
     * Useful for monitoring and testing
     */
    fun getStoredCount(): Int
}

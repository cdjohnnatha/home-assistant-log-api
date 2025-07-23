package com.homeassistant.infra.filter

import com.homeassistant.domain.model.EventHash
import com.homeassistant.domain.model.EventLog
import com.homeassistant.domain.port.DuplicateFilterPort
import com.homeassistant.infra.extensions.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of DuplicateFilterPort
 * Uses ConcurrentHashMap for thread-safety and automatic cleanup via scheduled task
 */
@Component
class InMemoryDuplicateFilterAdapter(
    @Value("\${duplicate-filter.ttl-minutes:5}")
    private val ttlMinutes: Long,
    @Value("\${duplicate-filter.enabled:true}")
    private val enabled: Boolean,
    @Value("\${duplicate-filter.max-cache-size:1000}")
    private val maxCacheSize: Int,
) : DuplicateFilterPort {
    private val eventHashes = ConcurrentHashMap<String, EventHash>()

    override fun isDuplicate(event: EventLog): Boolean {
        if (!enabled) {
            logger.debug("Duplicate filter is disabled, allowing event")
            return false
        }

        val eventHash = EventHash.from(event)
        val existingHash = eventHashes[eventHash.hash]

        if (existingHash == null) {
            logger.debug("Event hash not found, not a duplicate: ${eventHash.hash}")
            return false
        }

        if (existingHash.isExpired(ttlMinutes * 60)) {
            logger.debug("Event hash expired, removing and allowing: ${eventHash.hash}")
            eventHashes.remove(eventHash.hash)
            return false
        }

        logger.info("Duplicate event detected and blocked: source=${event.source}, type=${event.eventType}")
        return true
    }

    override fun recordEvent(eventHash: EventHash) {
        if (!enabled) {
            return
        }

        // Enforce cache size limit
        if (eventHashes.size >= maxCacheSize) {
            logger.warn("Cache size limit reached ($maxCacheSize), forcing cleanup")
            cleanup()
        }

        eventHashes[eventHash.hash] = eventHash
        logger.debug("Recorded event hash: ${eventHash.hash}")
    }

    @Scheduled(fixedRateString = "\${duplicate-filter.cleanup-interval-minutes:10}000")
    override fun cleanup() {
        if (!enabled) {
            return
        }

        val initialSize = eventHashes.size
        val ttlSeconds = ttlMinutes * 60

        val iterator = eventHashes.entries.iterator()
        var removedCount = 0

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.isExpired(ttlSeconds)) {
                iterator.remove()
                removedCount++
            }
        }

        if (removedCount > 0) {
            logger.info("Cleanup completed: removed $removedCount expired entries, cache size: $initialSize -> ${eventHashes.size}")
        }
    }

    override fun getStoredCount(): Int {
        return eventHashes.size
    }
}

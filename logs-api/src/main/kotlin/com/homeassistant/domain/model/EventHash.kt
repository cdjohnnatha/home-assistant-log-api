package com.homeassistant.domain.model

import java.security.MessageDigest
import java.time.Instant

/**
 * Represents a unique hash for an event used in duplicate detection
 */
data class EventHash(
    val hash: String,
    val source: String,
    val eventType: String,
    val createdAt: Instant = Instant.now(),
) {
    companion object {
        /**
         * Creates an EventHash from an EventLog
         * Uses SHA-256 to generate a deterministic hash from event content
         */
        fun from(event: EventLog): EventHash {
            val content = "${event.source}:${event.eventType}:${event.payload}"
            val hash =
                MessageDigest.getInstance("SHA-256")
                    .digest(content.toByteArray())
                    .joinToString("") { "%02x".format(it) }

            return EventHash(
                hash = hash,
                source = event.source,
                eventType = event.eventType.name,
            )
        }
    }

    /**
     * Checks if this hash has expired based on TTL
     */
    fun isExpired(ttlSeconds: Long): Boolean {
        val expirationTime = createdAt.plusSeconds(ttlSeconds)
        return Instant.now().isAfter(expirationTime)
    }
}

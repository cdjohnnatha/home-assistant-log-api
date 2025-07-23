package com.homeassistant.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Represents an event that can be retried upon failure
 * Used by the retry manager to track and schedule retry attempts
 */
data class RetryableEvent(
    val id: String = UUID.randomUUID().toString(),
    val originalEvent: EventLog,
    val message: String,
    val attemptCount: Int = 0,
    val maxAttempts: Int = 3,
    val nextRetryAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now(),
    val lastError: String? = null,
) {
    /**
     * Checks if this event has exceeded the maximum retry attempts
     */
    fun hasExceededMaxAttempts(): Boolean {
        return attemptCount >= maxAttempts
    }

    /**
     * Checks if this event is ready for retry based on the scheduled time
     */
    fun isReadyForRetry(): Boolean {
        return Instant.now().isAfter(nextRetryAt) || Instant.now().equals(nextRetryAt)
    }

    /**
     * Creates a new RetryableEvent for the next retry attempt
     * Uses exponential backoff for delay calculation
     */
    fun scheduleNextRetry(
        error: String,
        baseDelaySeconds: Long = 1,
        backoffMultiplier: Double = 2.0,
    ): RetryableEvent {
        val nextAttempt = attemptCount + 1
        val delaySeconds = (baseDelaySeconds * Math.pow(backoffMultiplier, nextAttempt - 1.0)).toLong()
        val nextRetryTime = Instant.now().plusSeconds(delaySeconds)

        return copy(
            attemptCount = nextAttempt,
            nextRetryAt = nextRetryTime,
            lastError = error,
        )
    }

    /**
     * Checks if this retry event has expired and should be cleaned up
     */
    fun isExpired(maxAgeHours: Long = 24): Boolean {
        val expirationTime = createdAt.plusSeconds(maxAgeHours * 3600)
        return Instant.now().isAfter(expirationTime)
    }
} 
package com.homeassistant.domain.port

import com.homeassistant.domain.model.RetryableEvent

/**
 * Port for managing retryable events
 * Implementation should handle scheduling, tracking, and execution of retry attempts
 */
interface RetryManagerPort {
    /**
     * Schedules an event for retry
     * @param retryableEvent The event to be retried
     */
    fun scheduleRetry(retryableEvent: RetryableEvent)

    /**
     * Gets all events that are ready for retry based on their scheduled time
     * @return List of events ready to be retried
     */
    fun getEventsReadyForRetry(): List<RetryableEvent>

    /**
     * Marks a retry attempt as completed (either success or final failure)
     * @param eventId The ID of the event that was processed
     * @param success Whether the retry was successful
     * @param error Error message if the retry failed
     */
    fun markRetryCompleted(
        eventId: String,
        success: Boolean,
        error: String? = null,
    )

    /**
     * Removes expired retry events to prevent memory leaks
     * Should be called periodically to clean up old events
     */
    fun cleanup()

    /**
     * Gets the current count of scheduled retry events
     * Useful for monitoring and testing
     */
    fun getRetryCount(): Int

    /**
     * Gets retry statistics for monitoring
     * @return Map with retry statistics (total, pending, failed, etc.)
     */
    fun getRetryStats(): Map<String, Long>
} 
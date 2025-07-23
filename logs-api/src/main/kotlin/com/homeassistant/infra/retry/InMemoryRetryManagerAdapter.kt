package com.homeassistant.infra.retry

import com.homeassistant.domain.model.RetryableEvent
import com.homeassistant.domain.port.RetryManagerPort
import com.homeassistant.infra.extensions.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of RetryManagerPort
 * Uses ConcurrentHashMap for thread-safety and scheduled cleanup
 */
@Component
class InMemoryRetryManagerAdapter(
    @Value("\${retry-policy.enabled:true}")
    private val enabled: Boolean,
    @Value("\${retry-policy.max-attempts:3}")
    private val maxAttempts: Int,
    @Value("\${retry-policy.initial-delay-seconds:1}")
    private val initialDelaySeconds: Long,
    @Value("\${retry-policy.backoff-multiplier:2.0}")
    private val backoffMultiplier: Double,
) : RetryManagerPort {
    private val scheduledRetries = ConcurrentHashMap<String, RetryableEvent>()
    private val completedRetries = ConcurrentHashMap<String, RetryableEvent>()

    override fun scheduleRetry(retryableEvent: RetryableEvent) {
        if (!enabled) {
            logger.debug("Retry is disabled, not scheduling retry for event: ${retryableEvent.id}")
            return
        }

        if (retryableEvent.hasExceededMaxAttempts()) {
            logger.warn(
                "Event ${retryableEvent.id} has exceeded max attempts (${retryableEvent.maxAttempts}), " +
                    "marking as permanently failed",
            )
            markRetryCompleted(retryableEvent.id, success = false, error = "Max attempts exceeded")
            return
        }

        scheduledRetries[retryableEvent.id] = retryableEvent
        logger.info(
            "Scheduled retry for event ${retryableEvent.id}, attempt ${retryableEvent.attemptCount + 1}/${retryableEvent.maxAttempts}, " +
                "next retry at: ${retryableEvent.nextRetryAt}",
        )
    }

    override fun getEventsReadyForRetry(): List<RetryableEvent> {
        if (!enabled) {
            return emptyList()
        }

        val readyEvents = scheduledRetries.values.filter { it.isReadyForRetry() }

        if (readyEvents.isNotEmpty()) {
            logger.debug("Found ${readyEvents.size} events ready for retry")
        }

        return readyEvents
    }

    override fun markRetryCompleted(
        eventId: String,
        success: Boolean,
        error: String?,
    ) {
        val event = scheduledRetries.remove(eventId)

        if (event == null) {
            logger.warn("Attempted to mark non-existent retry event as completed: $eventId")
            return
        }

        if (success) {
            logger.info("Retry successful for event $eventId after ${event.attemptCount + 1} attempts")
            completedRetries[eventId] = event.copy(lastError = null)
        } else {
            val updatedEvent =
                if (error != null) {
                    event.copy(lastError = error)
                } else {
                    event
                }

            if (event.hasExceededMaxAttempts()) {
                logger.error(
                    "Event $eventId permanently failed after ${event.attemptCount} attempts. " +
                        "Last error: ${error ?: event.lastError}",
                )
                completedRetries[eventId] = updatedEvent
            } else {
                // Schedule next retry with exponential backoff
                val nextRetry =
                    event.scheduleNextRetry(
                        error = error ?: "Unknown error",
                        baseDelaySeconds = initialDelaySeconds,
                        backoffMultiplier = backoffMultiplier,
                    )
                scheduleRetry(nextRetry)
            }
        }
    }

    @Scheduled(fixedRateString = "\${retry-policy.cleanup-interval-minutes:60}000")
    override fun cleanup() {
        if (!enabled) {
            return
        }

        val initialScheduledSize = scheduledRetries.size
        val initialCompletedSize = completedRetries.size

        // Remove expired scheduled retries
        val expiredScheduled = scheduledRetries.values.filter { it.isExpired() }
        expiredScheduled.forEach { event ->
            scheduledRetries.remove(event.id)
            logger.info("Removed expired scheduled retry: ${event.id}")
        }

        // Remove old completed retries
        val expiredCompleted = completedRetries.values.filter { it.isExpired() }
        expiredCompleted.forEach { event ->
            completedRetries.remove(event.id)
        }

        val removedScheduled = expiredScheduled.size
        val removedCompleted = expiredCompleted.size

        if (removedScheduled > 0 || removedCompleted > 0) {
            logger.info(
                "Cleanup completed: removed $removedScheduled scheduled retries, $removedCompleted completed retries. " +
                    "Current counts: ${scheduledRetries.size} scheduled, ${completedRetries.size} completed",
            )
        }
    }

    override fun getRetryCount(): Int {
        return scheduledRetries.size
    }

    override fun getRetryStats(): Map<String, Long> {
        val scheduledByAttempt = scheduledRetries.values.groupBy { it.attemptCount }.mapValues { it.value.size.toLong() }
        val totalCompleted = completedRetries.size.toLong()
        val totalSuccessful = completedRetries.values.count { it.lastError == null }.toLong()
        val totalFailed = totalCompleted - totalSuccessful

        return mapOf(
            "scheduled_total" to scheduledRetries.size.toLong(),
            "scheduled_attempt_1" to (scheduledByAttempt[1] ?: 0L),
            "scheduled_attempt_2" to (scheduledByAttempt[2] ?: 0L),
            "scheduled_attempt_3" to (scheduledByAttempt[3] ?: 0L),
            "completed_total" to totalCompleted,
            "completed_successful" to totalSuccessful,
            "completed_failed" to totalFailed,
        )
    }
}

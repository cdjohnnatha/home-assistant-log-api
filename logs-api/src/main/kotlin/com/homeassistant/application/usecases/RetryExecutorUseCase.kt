package com.homeassistant.application.usecases

import com.homeassistant.domain.port.RetryManagerPort
import com.homeassistant.infra.extensions.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * Use case responsible for executing scheduled retry attempts
 * Periodically checks for events ready for retry and processes them
 */
@Service
class RetryExecutorUseCase(
    private val retryManager: RetryManagerPort,
    private val notificationPublisher: NotificationPublisherUseCase,
    @Value("\${retry-policy.enabled:true}")
    private val retryEnabled: Boolean,
) {
    /**
     * Processes all events that are ready for retry
     * Called periodically by Spring's scheduler
     */
    @Scheduled(fixedRateString = "\${retry-policy.execution-interval-seconds:30}000")
    fun processScheduledRetries() {
        if (!retryEnabled) {
            return
        }

        val eventsToRetry = retryManager.getEventsReadyForRetry()

        if (eventsToRetry.isEmpty()) {
            logger.debug("No events ready for retry")
            return
        }

        logger.info("Processing ${eventsToRetry.size} events ready for retry")

        eventsToRetry.forEach { retryableEvent ->
            try {
                logger.info(
                    "Attempting retry ${retryableEvent.attemptCount + 1}/${retryableEvent.maxAttempts} " +
                        "for event ${retryableEvent.id} from source: ${retryableEvent.originalEvent.source}",
                )

                // Attempt to send the notification
                val success = notificationPublisher.execute(retryableEvent.message)

                if (success) {
                    // Mark as successful - no more retries needed
                    retryManager.markRetryCompleted(
                        eventId = retryableEvent.id,
                        success = true,
                    )
                    logger.info(
                        "Retry successful for event ${retryableEvent.id} " +
                            "after ${retryableEvent.attemptCount + 1} attempts",
                    )
                } else {
                    // Mark as failed - may schedule another retry if attempts remain
                    retryManager.markRetryCompleted(
                        eventId = retryableEvent.id,
                        success = false,
                        error = "Notification publisher returned false",
                    )
                }
            } catch (exception: Exception) {
                logger.error(
                    "Exception during retry attempt for event ${retryableEvent.id}: ${exception.message}",
                    exception,
                )

                // Mark as failed with exception details
                retryManager.markRetryCompleted(
                    eventId = retryableEvent.id,
                    success = false,
                    error = "Exception: ${exception.message}",
                )
            }
        }
    }

    /**
     * Manually processes a specific retry event
     * Useful for testing or manual intervention
     */
    fun processSpecificRetry(eventId: String): Boolean {
        if (!retryEnabled) {
            logger.warn("Retry is disabled, cannot process specific retry for event: $eventId")
            return false
        }

        val eventsToRetry = retryManager.getEventsReadyForRetry()
        val specificEvent = eventsToRetry.find { it.id == eventId }

        if (specificEvent == null) {
            logger.warn("Event $eventId not found in ready-for-retry list")
            return false
        }

        return try {
            val success = notificationPublisher.execute(specificEvent.message)
            retryManager.markRetryCompleted(
                eventId = specificEvent.id,
                success = success,
                error = if (success) null else "Manual retry failed",
            )
            logger.info("Manual retry for event $eventId completed with success: $success")
            success
        } catch (exception: Exception) {
            retryManager.markRetryCompleted(
                eventId = specificEvent.id,
                success = false,
                error = "Manual retry exception: ${exception.message}",
            )
            logger.error("Manual retry for event $eventId failed with exception", exception)
            false
        }
    }

    /**
     * Gets current retry statistics for monitoring
     */
    fun getRetryStatistics(): Map<String, Long> {
        return retryManager.getRetryStats()
    }
} 
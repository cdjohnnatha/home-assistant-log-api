package com.homeassistant.application.usecases

import com.homeassistant.domain.model.EventLog
import com.homeassistant.domain.model.RetryableEvent
import com.homeassistant.domain.port.NotificationPublisherPort
import com.homeassistant.domain.port.RetryManagerPort
import com.homeassistant.infra.extensions.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class NotificationPublisherUseCase(
    private val notificationPublisher: NotificationPublisherPort,
    private val retryManager: RetryManagerPort,
    @Value("\${retry-policy.enabled:true}")
    private val retryEnabled: Boolean,
) {
    /**
     * Publishes a notification message
     * If it fails and retry is enabled, schedules for retry
     */
    fun execute(message: String): Boolean {
        logger.debug("Executing notification publish for message: ${message.take(50)}...")

        val result = notificationPublisher.publish(message)

        if (result) {
            logger.debug("Notification published successfully")
        } else {
            logger.warn("Failed to publish notification")
        }

        return result
    }

    /**
     * Publishes a notification for an event with retry support
     * If initial attempt fails, schedules the event for retry
     */
    fun executeWithRetry(
        event: EventLog,
        message: String,
    ): Boolean {
        logger.debug("Executing notification publish with retry for event from: ${event.source}")

        val result = notificationPublisher.publish(message)

        if (result) {
            logger.debug("Notification published successfully for event from: ${event.source}")
            return true
        }

        // Initial attempt failed
        logger.warn("Initial notification publish failed for event from: ${event.source}")

        if (!retryEnabled) {
            logger.debug("Retry is disabled, not scheduling retry for failed notification")
            return false
        }

        // Schedule for retry
        try {
            // This was the first attempt
            val retryableEvent =
                RetryableEvent(
                    originalEvent = event,
                    message = message,
                    attemptCount = 0,
                    maxAttempts = 3,
                )

            retryManager.scheduleRetry(retryableEvent)
            logger.info(
                "Scheduled retry for failed notification from source: ${event.source}, " +
                    "event ID: ${retryableEvent.id}",
            )

            // Return false to indicate initial attempt failed, but retry is scheduled
            return false
        } catch (exception: Exception) {
            logger.error(
                "Failed to schedule retry for event from: ${event.source}. " +
                    "Exception: ${exception.message}",
                exception,
            )
            return false
        }
    }
}

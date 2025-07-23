package com.homeassistant.application.usecases

import com.homeassistant.domain.model.EventHash
import com.homeassistant.domain.model.EventLog
import com.homeassistant.domain.port.DuplicateFilterPort
import com.homeassistant.infra.extensions.logger
import org.springframework.stereotype.Service

@Service
class ProcessEventUseCase(
    private val notificationPublisher: NotificationPublisherUseCase,
    private val duplicateFilter: DuplicateFilterPort,
) {
    fun execute(event: EventLog) {
        logger.info("Processing event: source=${event.source}, type=${event.eventType}")

        // Check for duplicates before processing
        if (duplicateFilter.isDuplicate(event)) {
            logger.info("Duplicate event detected and skipped: source=${event.source}, type=${event.eventType}")
            return
        }

        // Record this event to prevent future duplicates
        val eventHash = EventHash.from(event)
        duplicateFilter.recordEvent(eventHash)

        // Process the event normally
        val message = createNotificationMessage(event)
        val success = notificationPublisher.execute(message)

        if (success) {
            logger.info("Notification sent successfully for event from: ${event.source}")
        } else {
            logger.error("Failed to send notification for event from: ${event.source}")
        }
    }

    private fun createNotificationMessage(event: EventLog): String {
        return """
            Novo evento Home Assistant:
            Fonte: ${event.source}
            Tipo: ${event.eventType}
            Timestamp: ${event.timestamp}
            Payload: ${event.payload}
            """.trimIndent()
    }
}

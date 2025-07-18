package com.homeassistant.application.usecases

import com.homeassistant.domain.model.EventLog
import com.homeassistant.infra.extensions.logger
import org.springframework.stereotype.Service

@Service
class ProcessEventUseCase(private val notificationPublisher: NotificationPublisherUseCase) {
    fun execute(event: EventLog) {
        logger.info("event processed: $event")

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

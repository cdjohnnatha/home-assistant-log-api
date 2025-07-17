package com.homeassistant.application.usecases

import com.homeassistant.domain.port.NotificationPublisherPort
import com.homeassistant.infra.extensions.logger
import org.springframework.stereotype.Service

@Service
class NotificationPublisherUseCase(private val notificationPublisher: NotificationPublisherPort) {
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
}

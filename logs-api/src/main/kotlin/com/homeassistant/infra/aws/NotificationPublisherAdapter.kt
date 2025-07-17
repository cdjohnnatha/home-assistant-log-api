package com.homeassistant.infra.aws

import com.homeassistant.domain.port.NotificationPublisherPort
import com.homeassistant.infra.extensions.logger
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest

@Component
class NotificationPublisherAdapter(private val snsClient: SnsClient) : NotificationPublisherPort {
    private val topicArn = System.getenv("AWS_SNS_TOPIC_ARN")

    override fun publish(message: String): Boolean {
        return try {
            if (topicArn.isNullOrBlank()) {
                logger.error("AWS_SNS_TOPIC_ARN is missing on environment variables")
                return false
            }

            val request =
                PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(message)
                    .subject("Home-Assistant-Event")
                    .build()

            val response = snsClient.publish(request)
            logger.info("Notification sent successfully for event from: ${response.messageId()}")
            true
        } catch (e: Exception) {
            logger.error("Error sending SNS message: ${e.message}", e)
            false
        }
    }
}

package com.homeassistant.application.usecases

import com.homeassistant.domain.port.NotificationPublisherPort
import com.homeassistant.domain.port.RetryManagerPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("NotificationPublisherUseCase Tests")
class NotificationPublisherUseCaseTest {
    private lateinit var notificationPublisherPort: NotificationPublisherPort
    private lateinit var retryManagerPort: RetryManagerPort
    private lateinit var notificationPublisherUseCase: NotificationPublisherUseCase

    @BeforeEach
    fun setup() {
        notificationPublisherPort = mockk()
        retryManagerPort = mockk()
        notificationPublisherUseCase = NotificationPublisherUseCase(
            notificationPublisherPort,
            retryManagerPort,
            retryEnabled = true,
        )
    }

    @Nested
    @DisplayName("Successful Publishing")
    inner class SuccessfulPublishing {
        @Test
        @DisplayName("Should publish notification successfully and return true")
        fun `should publish notification successfully and return true`() {
            // Given
            val message = "Test notification message"
            every { notificationPublisherPort.publish(any()) } returns true

            // When
            val result = notificationPublisherUseCase.execute(message)

            // Then
            assertTrue(result)
            verify(exactly = 1) { notificationPublisherPort.publish(message) }
        }

        @Test
        @DisplayName("Should handle long messages correctly")
        fun `should handle long messages correctly`() {
            // Given
            val longMessage = "A".repeat(1000) // Mensagem longa
            every { notificationPublisherPort.publish(any()) } returns true

            // When
            val result = notificationPublisherUseCase.execute(longMessage)

            // Then
            assertTrue(result)
            verify(exactly = 1) { notificationPublisherPort.publish(longMessage) }
        }
    }

    @Nested
    @DisplayName("Failed Publishing")
    inner class FailedPublishing {
        @Test
        @DisplayName("Should return false when notification publishing fails")
        fun `should return false when notification publishing fails`() {
            // Given
            val message = "Test message"
            every { notificationPublisherPort.publish(any()) } returns false

            // When
            val result = notificationPublisherUseCase.execute(message)

            // Then
            assertFalse(result)
            verify(exactly = 1) { notificationPublisherPort.publish(message) }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {
        @Test
        @DisplayName("Should handle empty message")
        fun `should handle empty message`() {
            // Given
            val emptyMessage = ""
            every { notificationPublisherPort.publish(any()) } returns true

            // When
            val result = notificationPublisherUseCase.execute(emptyMessage)

            // Then
            assertTrue(result)
            verify(exactly = 1) { notificationPublisherPort.publish(emptyMessage) }
        }

        @Test
        @DisplayName("Should handle messages with special characters")
        fun `should handle messages with special characters`() {
            // Given
            val specialMessage = "Test with émojis 🚀 and special chars: àáâãäåæçèéêë"
            every { notificationPublisherPort.publish(any()) } returns true

            // When
            val result = notificationPublisherUseCase.execute(specialMessage)

            // Then
            assertTrue(result)
            verify(exactly = 1) { notificationPublisherPort.publish(specialMessage) }
        }
    }
}

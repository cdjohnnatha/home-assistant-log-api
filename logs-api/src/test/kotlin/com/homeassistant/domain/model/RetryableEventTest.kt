package com.homeassistant.domain.model

import com.homeassistant.domain.enum.EventLogType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("RetryableEvent Tests")
class RetryableEventTest {

    @Nested
    @DisplayName("Retry Attempt Management")
    inner class RetryAttemptManagementTests {

        @Test
        @DisplayName("Should not exceed max attempts initially")
        fun `should not exceed max attempts initially`() {
            // Given
            val retryableEvent = createTestRetryableEvent()

            // When & Then
            assertFalse(retryableEvent.hasExceededMaxAttempts())
        }

        @Test
        @DisplayName("Should exceed max attempts after 3 attempts")
        fun `should exceed max attempts after 3 attempts`() {
            // Given
            val retryableEvent = createTestRetryableEvent(attemptCount = 3)

            // When & Then
            assertTrue(retryableEvent.hasExceededMaxAttempts())
        }

        @Test
        @DisplayName("Should be ready for retry immediately when created")
        fun `should be ready for retry immediately when created`() {
            // Given
            val retryableEvent = createTestRetryableEvent()

            // When & Then
            assertTrue(retryableEvent.isReadyForRetry())
        }

        @Test
        @DisplayName("Should not be ready for retry when scheduled in future")
        fun `should not be ready for retry when scheduled in future`() {
            // Given
            val futureRetry = Instant.now().plusSeconds(60)
            val retryableEvent = createTestRetryableEvent(nextRetryAt = futureRetry)

            // When & Then
            assertFalse(retryableEvent.isReadyForRetry())
        }

        @Test
        @DisplayName("Should be ready for retry when scheduled time has passed")
        fun `should be ready for retry when scheduled time has passed`() {
            // Given
            val pastRetry = Instant.now().minusSeconds(10)
            val retryableEvent = createTestRetryableEvent(nextRetryAt = pastRetry)

            // When & Then
            assertTrue(retryableEvent.isReadyForRetry())
        }
    }

    @Nested
    @DisplayName("Exponential Backoff")
    inner class ExponentialBackoffTests {

        @Test
        @DisplayName("Should schedule first retry after 1 second")
        fun `should schedule first retry after 1 second`() {
            // Given
            val retryableEvent = createTestRetryableEvent(attemptCount = 0)
            val beforeScheduling = Instant.now()

            // When
            val nextRetry = retryableEvent.scheduleNextRetry("Test error")

            // Then
            assertEquals(1, nextRetry.attemptCount)
            assertEquals("Test error", nextRetry.lastError)
            assertTrue(nextRetry.nextRetryAt.isAfter(beforeScheduling))
            // Should be approximately 1 second later (allowing some tolerance)
            assertTrue(nextRetry.nextRetryAt.isBefore(beforeScheduling.plusSeconds(2)))
        }

        @Test
        @DisplayName("Should schedule second retry after 2 seconds")
        fun `should schedule second retry after 2 seconds`() {
            // Given
            val retryableEvent = createTestRetryableEvent(attemptCount = 1)
            val beforeScheduling = Instant.now()

            // When
            val nextRetry = retryableEvent.scheduleNextRetry("Second error")

            // Then
            assertEquals(2, nextRetry.attemptCount)
            assertEquals("Second error", nextRetry.lastError)
            // Should be approximately 2 seconds later
            assertTrue(nextRetry.nextRetryAt.isAfter(beforeScheduling.plusSeconds(1)))
            assertTrue(nextRetry.nextRetryAt.isBefore(beforeScheduling.plusSeconds(3)))
        }

        @Test
        @DisplayName("Should schedule third retry after 4 seconds")
        fun `should schedule third retry after 4 seconds`() {
            // Given
            val retryableEvent = createTestRetryableEvent(attemptCount = 2)
            val beforeScheduling = Instant.now()

            // When
            val nextRetry = retryableEvent.scheduleNextRetry("Third error")

            // Then
            assertEquals(3, nextRetry.attemptCount)
            assertEquals("Third error", nextRetry.lastError)
            // Should be approximately 4 seconds later
            assertTrue(nextRetry.nextRetryAt.isAfter(beforeScheduling.plusSeconds(3)))
            assertTrue(nextRetry.nextRetryAt.isBefore(beforeScheduling.plusSeconds(5)))
        }

        @Test
        @DisplayName("Should use custom backoff multiplier")
        fun `should use custom backoff multiplier`() {
            // Given
            val retryableEvent = createTestRetryableEvent(attemptCount = 1)
            val beforeScheduling = Instant.now()

            // When - Using 3.0 multiplier instead of 2.0
            val nextRetry = retryableEvent.scheduleNextRetry(
                error = "Custom backoff error",
                baseDelaySeconds = 1,
                backoffMultiplier = 3.0,
            )

            // Then
            assertEquals(2, nextRetry.attemptCount)
            // With 3.0 multiplier: 1 * 3^(2-1) = 3 seconds
            assertTrue(nextRetry.nextRetryAt.isAfter(beforeScheduling.plusSeconds(2)))
            assertTrue(nextRetry.nextRetryAt.isBefore(beforeScheduling.plusSeconds(4)))
        }

        @Test
        @DisplayName("Should use custom base delay")
        fun `should use custom base delay`() {
            // Given
            val retryableEvent = createTestRetryableEvent(attemptCount = 0)
            val beforeScheduling = Instant.now()

            // When - Using 5 seconds base delay
            val nextRetry = retryableEvent.scheduleNextRetry(
                error = "Custom delay error",
                baseDelaySeconds = 5,
                backoffMultiplier = 2.0,
            )

            // Then
            assertEquals(1, nextRetry.attemptCount)
            // With 5 seconds base: 5 * 2^(1-1) = 5 seconds
            assertTrue(nextRetry.nextRetryAt.isAfter(beforeScheduling.plusSeconds(4)))
            assertTrue(nextRetry.nextRetryAt.isBefore(beforeScheduling.plusSeconds(6)))
        }
    }

    @Nested
    @DisplayName("Event Expiration")
    inner class EventExpirationTests {

        @Test
        @DisplayName("Should not be expired immediately after creation")
        fun `should not be expired immediately after creation`() {
            // Given
            val retryableEvent = createTestRetryableEvent()

            // When & Then
            assertFalse(retryableEvent.isExpired())
        }

        @Test
        @DisplayName("Should be expired after 24 hours")
        fun `should be expired after 24 hours`() {
            // Given
            val oldCreationTime = Instant.now().minusSeconds(25 * 3600) // 25 hours ago
            val retryableEvent = createTestRetryableEvent(createdAt = oldCreationTime)

            // When & Then
            assertTrue(retryableEvent.isExpired())
        }

        @Test
        @DisplayName("Should use custom expiration time")
        fun `should use custom expiration time`() {
            // Given
            val oldCreationTime = Instant.now().minusSeconds(2 * 3600) // 2 hours ago
            val retryableEvent = createTestRetryableEvent(createdAt = oldCreationTime)

            // When & Then
            assertTrue(retryableEvent.isExpired(maxAgeHours = 1)) // 1 hour max age
            assertFalse(retryableEvent.isExpired(maxAgeHours = 3)) // 3 hours max age
        }
    }

    @Nested
    @DisplayName("Event Properties")
    inner class EventPropertiesTests {

        @Test
        @DisplayName("Should preserve original event data")
        fun `should preserve original event data`() {
            // Given
            val originalEvent = createTestEvent()
            val message = "Test notification message"

            // When
            val retryableEvent = RetryableEvent(
                originalEvent = originalEvent,
                message = message,
            )

            // Then
            assertEquals(originalEvent.source, retryableEvent.originalEvent.source)
            assertEquals(originalEvent.eventType, retryableEvent.originalEvent.eventType)
            assertEquals(originalEvent.payload, retryableEvent.originalEvent.payload)
            assertEquals(message, retryableEvent.message)
        }

        @Test
        @DisplayName("Should generate unique IDs for different events")
        fun `should generate unique IDs for different events`() {
            // Given
            val event1 = createTestRetryableEvent()
            val event2 = createTestRetryableEvent()

            // When & Then
            assertFalse(event1.id == event2.id)
            assertTrue(event1.id.isNotBlank())
            assertTrue(event2.id.isNotBlank())
        }

        @Test
        @DisplayName("Should use default values correctly")
        fun `should use default values correctly`() {
            // Given
            val originalEvent = createTestEvent()
            val message = "Test message"

            // When
            val retryableEvent = RetryableEvent(
                originalEvent = originalEvent,
                message = message,
            )

            // Then
            assertEquals(0, retryableEvent.attemptCount)
            assertEquals(3, retryableEvent.maxAttempts)
            assertEquals(null, retryableEvent.lastError)
            assertTrue(retryableEvent.id.isNotBlank())
        }
    }

    private fun createTestRetryableEvent(
        attemptCount: Int = 0,
        maxAttempts: Int = 3,
        nextRetryAt: Instant = Instant.now(),
        createdAt: Instant = Instant.now(),
        lastError: String? = null,
    ): RetryableEvent {
        return RetryableEvent(
            originalEvent = createTestEvent(),
            message = "Test notification message",
            attemptCount = attemptCount,
            maxAttempts = maxAttempts,
            nextRetryAt = nextRetryAt,
            createdAt = createdAt,
            lastError = lastError,
        )
    }

    private fun createTestEvent(): EventLog {
        return EventLog(
            source = "test-source",
            eventType = EventLogType.INFO,
            timestamp = Instant.now(),
            payload = mapOf("test" to "data"),
        )
    }
} 
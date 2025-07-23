package com.homeassistant.domain.model

import com.homeassistant.domain.enum.EventLogType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@DisplayName("EventHash Tests")
class EventHashTest {
    @Nested
    @DisplayName("Hash Generation")
    inner class HashGenerationTests {
        @Test
        @DisplayName("Should generate consistent hash for identical events")
        fun `should generate consistent hash for identical events`() {
            // Given
            val event1 = createTestEvent()
            val event2 = createTestEvent()

            // When
            val hash1 = EventHash.from(event1)
            val hash2 = EventHash.from(event2)

            // Then
            assertEquals(hash1.hash, hash2.hash)
            assertEquals(hash1.source, hash2.source)
            assertEquals(hash1.eventType, hash2.eventType)
        }

        @Test
        @DisplayName("Should generate different hashes for different sources")
        fun `should generate different hashes for different sources`() {
            // Given
            val event1 = createTestEvent(source = "source1")
            val event2 = createTestEvent(source = "source2")

            // When
            val hash1 = EventHash.from(event1)
            val hash2 = EventHash.from(event2)

            // Then
            assertNotEquals(hash1.hash, hash2.hash)
        }

        @Test
        @DisplayName("Should generate different hashes for different event types")
        fun `should generate different hashes for different event types`() {
            // Given
            val event1 = createTestEvent(eventType = EventLogType.INFO)
            val event2 = createTestEvent(eventType = EventLogType.ERROR)

            // When
            val hash1 = EventHash.from(event1)
            val hash2 = EventHash.from(event2)

            // Then
            assertNotEquals(hash1.hash, hash2.hash)
        }

        @Test
        @DisplayName("Should generate different hashes for different payloads")
        fun `should generate different hashes for different payloads`() {
            // Given
            val event1 = createTestEvent(payload = mapOf("key" to "value1"))
            val event2 = createTestEvent(payload = mapOf("key" to "value2"))

            // When
            val hash1 = EventHash.from(event1)
            val hash2 = EventHash.from(event2)

            // Then
            assertNotEquals(hash1.hash, hash2.hash)
        }

        @Test
        @DisplayName("Should generate valid SHA-256 hash")
        fun `should generate valid SHA-256 hash`() {
            // Given
            val event = createTestEvent()

            // When
            val eventHash = EventHash.from(event)

            // Then
            assertEquals(64, eventHash.hash.length) // SHA-256 produces 64 hex characters
            assertTrue(eventHash.hash.matches(Regex("[a-f0-9]+"))) // Should be hex only
        }
    }

    @Nested
    @DisplayName("TTL Functionality")
    inner class TTLTests {
        @Test
        @DisplayName("Should not be expired immediately after creation")
        fun `should not be expired immediately after creation`() {
            // Given
            val eventHash = EventHash.from(createTestEvent())

            // When & Then
            assertFalse(eventHash.isExpired(300)) // 5 minutes TTL
        }

        @Test
        @DisplayName("Should be expired when TTL is 0")
        fun `should be expired when TTL is 0`() {
            // Given
            val eventHash = EventHash.from(createTestEvent())

            // When & Then
            assertTrue(eventHash.isExpired(0))
        }

        @Test
        @DisplayName("Should not be expired within TTL window")
        fun `should not be expired within TTL window`() {
            // Given
            val eventHash = EventHash.from(createTestEvent())

            // When & Then
            assertFalse(eventHash.isExpired(60)) // 1 minute TTL, should not be expired immediately
        }

        @Test
        @DisplayName("Should be expired for past creation time")
        fun `should be expired for past creation time`() {
            // Given
            val pastTime = Instant.now().minusSeconds(600) // 10 minutes ago
            val eventHash =
                EventHash(
                    hash = "test-hash",
                    source = "test-source",
                    eventType = "TEST",
                    createdAt = pastTime,
                )

            // When & Then
            assertTrue(eventHash.isExpired(300)) // 5 minutes TTL
        }
    }

    private fun createTestEvent(
        source: String = "test-source",
        eventType: EventLogType = EventLogType.INFO,
        payload: Map<String, Any> = mapOf("test" to "data"),
    ): EventLog {
        return EventLog(
            source = source,
            eventType = eventType,
            timestamp = Instant.now(),
            payload = payload,
        )
    }
}

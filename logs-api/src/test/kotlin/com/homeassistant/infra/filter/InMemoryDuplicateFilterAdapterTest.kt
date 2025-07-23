package com.homeassistant.infra.filter

import com.homeassistant.domain.enum.EventLogType
import com.homeassistant.domain.model.EventHash
import com.homeassistant.domain.model.EventLog
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("InMemoryDuplicateFilterAdapter Tests")
class InMemoryDuplicateFilterAdapterTest {
    private lateinit var duplicateFilter: InMemoryDuplicateFilterAdapter

    @BeforeEach
    fun setUp() {
        // Initialize with test configuration
        duplicateFilter =
            InMemoryDuplicateFilterAdapter(
                ttlMinutes = 5L,
                enabled = true,
                maxCacheSize = 100,
            )
    }

    @Nested
    @DisplayName("Duplicate Detection")
    inner class DuplicateDetectionTests {
        @Test
        @DisplayName("Should allow first occurrence of event")
        fun `should allow first occurrence of event`() {
            // Given
            val event = createTestEvent()

            // When
            val isDuplicate = duplicateFilter.isDuplicate(event)

            // Then
            assertFalse(isDuplicate)
        }

        @Test
        @DisplayName("Should detect duplicate event")
        fun `should detect duplicate event`() {
            // Given
            val event = createTestEvent()
            val eventHash = EventHash.from(event)

            // Record the event first
            duplicateFilter.recordEvent(eventHash)

            // When
            val isDuplicate = duplicateFilter.isDuplicate(event)

            // Then
            assertTrue(isDuplicate)
        }

        @Test
        @DisplayName("Should allow different events")
        fun `should allow different events`() {
            // Given
            val event1 = createTestEvent(source = "source1")
            val event2 = createTestEvent(source = "source2")
            val eventHash1 = EventHash.from(event1)

            // Record first event
            duplicateFilter.recordEvent(eventHash1)

            // When
            val isDuplicate = duplicateFilter.isDuplicate(event2)

            // Then
            assertFalse(isDuplicate)
        }

        @Test
        @DisplayName("Should allow expired events")
        fun `should allow expired events`() {
            // Given
            val event = createTestEvent()
            // Hash that's older than 5 minutes TTL
            val expiredHash =
                EventHash(
                    hash = EventHash.from(event).hash,
                    source = event.source,
                    eventType = event.eventType.name,
                    createdAt = Instant.now().minusSeconds(400),
                )

            // Record expired event
            duplicateFilter.recordEvent(expiredHash)

            // When
            val isDuplicate = duplicateFilter.isDuplicate(event)

            // Then
            assertFalse(isDuplicate)
        }
    }

    @Nested
    @DisplayName("Filter Disabled")
    inner class FilterDisabledTests {
        @Test
        @DisplayName("Should allow all events when disabled")
        fun `should allow all events when disabled`() {
            // Given
            val disabledFilter =
                InMemoryDuplicateFilterAdapter(
                    ttlMinutes = 5L,
                    enabled = false,
                    maxCacheSize = 100,
                )
            val event = createTestEvent()
            val eventHash = EventHash.from(event)

            // Record the event
            disabledFilter.recordEvent(eventHash)

            // When
            val isDuplicate = disabledFilter.isDuplicate(event)

            // Then
            assertFalse(isDuplicate)
        }
    }

    @Nested
    @DisplayName("Cache Management")
    inner class CacheManagementTests {
        @Test
        @DisplayName("Should track stored count correctly")
        fun `should track stored count correctly`() {
            // Given
            val event1 = createTestEvent(source = "source1")
            val event2 = createTestEvent(source = "source2")

            // When
            duplicateFilter.recordEvent(EventHash.from(event1))
            assertEquals(1, duplicateFilter.getStoredCount())

            duplicateFilter.recordEvent(EventHash.from(event2))
            assertEquals(2, duplicateFilter.getStoredCount())
        }

        @Test
        @DisplayName("Should not increase count when disabled")
        fun `should not increase count when disabled`() {
            // Given
            val disabledFilter =
                InMemoryDuplicateFilterAdapter(
                    ttlMinutes = 5L,
                    enabled = false,
                    maxCacheSize = 100,
                )
            val event = createTestEvent()

            // When
            disabledFilter.recordEvent(EventHash.from(event))

            // Then
            assertEquals(0, disabledFilter.getStoredCount())
        }

        @Test
        @DisplayName("Should cleanup expired entries")
        fun `should cleanup expired entries`() {
            // Given
            val event = createTestEvent()
            // Hash that's older than TTL
            val expiredHash =
                EventHash(
                    hash = "expired-hash",
                    source = "expired-source",
                    eventType = "EXPIRED",
                    createdAt = Instant.now().minusSeconds(400),
                )
            val recentHash = EventHash.from(event)

            // Record both hashes
            duplicateFilter.recordEvent(expiredHash)
            duplicateFilter.recordEvent(recentHash)
            assertEquals(2, duplicateFilter.getStoredCount())

            // When
            duplicateFilter.cleanup()

            // Then
            // Only recent hash should remain
            assertEquals(1, duplicateFilter.getStoredCount())
        }

        @Test
        @DisplayName("Should handle empty cache cleanup gracefully")
        fun `should handle empty cache cleanup gracefully`() {
            // Given - empty cache

            // When & Then - should not throw exception
            duplicateFilter.cleanup()
            assertEquals(0, duplicateFilter.getStoredCount())
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCaseTests {
        @Test
        @DisplayName("Should handle TTL of 0 minutes")
        fun `should handle TTL of 0 minutes`() {
            // Given
            val zeroTtlFilter =
                InMemoryDuplicateFilterAdapter(
                    ttlMinutes = 0L,
                    enabled = true,
                    maxCacheSize = 100,
                )
            val event = createTestEvent()
            val eventHash = EventHash.from(event)

            // When
            zeroTtlFilter.recordEvent(eventHash)
            val isDuplicate = zeroTtlFilter.isDuplicate(event)

            // Then
            // Should be expired immediately with 0 TTL
            assertFalse(isDuplicate)
        }

        @Test
        @DisplayName("Should handle very large payloads")
        fun `should handle very large payloads`() {
            // Given
            val largePayload = mutableMapOf<String, Any>()
            repeat(1000) { i ->
                largePayload["key$i"] = "value$i".repeat(100)
            }
            val event = createTestEvent(payload = largePayload)

            // When & Then - should not throw exception
            val isDuplicate = duplicateFilter.isDuplicate(event)
            assertFalse(isDuplicate)

            duplicateFilter.recordEvent(EventHash.from(event))
            assertEquals(1, duplicateFilter.getStoredCount())
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

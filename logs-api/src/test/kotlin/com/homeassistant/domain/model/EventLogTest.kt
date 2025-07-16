package com.homeassistant.domain.model

import com.homeassistant.domain.enum.EventLogType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("EventLog Domain Model Tests")
class EventLogTest {
    @Nested
    @DisplayName("EventLog Creation")
    inner class EventLogCreation {
        @Test
        @DisplayName("Should create EventLog with all properties")
        fun `should create event log with all properties`() {
            // Given
            val source = "home-assistant-core"
            val eventType = EventLogType.USER_ACTION
            val timestamp = Instant.now()
            val payload = mapOf("action" to "turn_on_light", "entity_id" to "light.living_room")

            // When
            val eventLog =
                EventLog(
                    source = source,
                    eventType = eventType,
                    timestamp = timestamp,
                    payload = payload,
                )

            // Then
            assertEquals(source, eventLog.source)
            assertEquals(eventType, eventLog.eventType)
            assertEquals(timestamp, eventLog.timestamp)
            assertEquals(payload, eventLog.payload)
        }

        @Test
        @DisplayName("Should create EventLog with empty payload")
        fun `should create event log with empty payload`() {
            // Given & When
            val eventLog =
                EventLog(
                    source = "test-source",
                    eventType = EventLogType.SYSTEM_EVENT,
                    timestamp = Instant.now(),
                    payload = emptyMap(),
                )

            // Then
            assertTrue(eventLog.payload.isEmpty())
            assertNotNull(eventLog.payload)
        }
    }

    @Nested
    @DisplayName("Data Class Behavior")
    inner class DataClassBehavior {
        @Test
        @DisplayName("Should support copy functionality")
        fun `should support copy functionality`() {
            // Given
            val originalEvent =
                EventLog(
                    source = "original-source",
                    eventType = EventLogType.INFO,
                    timestamp = Instant.now(),
                    payload = emptyMap(),
                )

            // When
            val copiedEvent = originalEvent.copy(source = "new-source")

            // Then
            assertEquals("new-source", copiedEvent.source)
            assertEquals(originalEvent.eventType, copiedEvent.eventType)
            assertEquals(originalEvent.timestamp, copiedEvent.timestamp)
            assertEquals(originalEvent.payload, copiedEvent.payload)
        }
    }

    @Nested
    @DisplayName("Payload Handling")
    inner class PayloadHandling {
        @Test
        @DisplayName("Should handle complex nested payload structures")
        fun `should handle complex nested payload structures`() {
            // Given
            val complexPayload =
                mapOf(
                    "entity_id" to "light.living_room",
                    "state" to
                        mapOf(
                            "brightness" to 255,
                            "color" to listOf(255, 0, 0),
                            "attributes" to
                                mapOf(
                                    "friendly_name" to "Living Room Light",
                                    "supported_features" to 63,
                                ),
                        ),
                    "metadata" to
                        mapOf(
                            "user_id" to "admin",
                            "context_id" to "abc123",
                            "timestamp" to "2024-01-01T10:00:00Z",
                        ),
                )

            // When
            val eventLog =
                EventLog(
                    source = "home-assistant",
                    eventType = EventLogType.USER_ACTION,
                    timestamp = Instant.now(),
                    payload = complexPayload,
                )

            // Then
            assertEquals("light.living_room", eventLog.payload["entity_id"])

            val state = eventLog.payload["state"] as Map<*, *>
            assertEquals(255, state["brightness"])
            assertTrue(state["color"] is List<*>)
            assertTrue(state["attributes"] is Map<*, *>)

            val metadata = eventLog.payload["metadata"] as Map<*, *>
            assertEquals("admin", metadata["user_id"])
        }

        @Test
        @DisplayName("Should handle payload with different data types")
        fun `should handle payload with different data types`() {
            // Given
            val mixedPayload =
                mapOf(
                    "string_value" to "test",
                    "int_value" to 42,
                    "double_value" to 3.14,
                    "boolean_value" to true,
                    "list_value" to listOf(1, 2, 3),
                    "map_value" to mapOf("nested" to "value"),
                )

            // When
            val eventLog =
                EventLog(
                    source = "test",
                    eventType = EventLogType.INFO,
                    timestamp = Instant.now(),
                    payload = mixedPayload,
                )

            // Then
            assertEquals("test", eventLog.payload["string_value"])
            assertEquals(42, eventLog.payload["int_value"])
            assertEquals(3.14, eventLog.payload["double_value"])
            assertEquals(true, eventLog.payload["boolean_value"])
            assertNull(eventLog.payload["null_value"])
            assertTrue(eventLog.payload["list_value"] is List<*>)
            assertTrue(eventLog.payload["map_value"] is Map<*, *>)
        }
    }
}

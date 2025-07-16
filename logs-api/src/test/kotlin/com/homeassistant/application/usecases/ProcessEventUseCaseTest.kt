package com.homeassistant.application.usecases

import com.homeassistant.domain.enum.EventLogType
import com.homeassistant.domain.model.EventLog
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Instant

@DisplayName("ProcessEventUseCase Tests")
class ProcessEventUseCaseTest {
    private val processEventUseCase = ProcessEventUseCase()

    @Nested
    @DisplayName("Event Processing")
    inner class EventProcessing {
        @Test
        @DisplayName("Should process event successfully without throwing exceptions")
        fun `should process event successfully without throwing exceptions`() {
            // Given
            val event =
                EventLog(
                    source = "home-assistant-core",
                    eventType = EventLogType.USER_ACTION,
                    timestamp = Instant.now(),
                    payload = mapOf("action" to "turn_on_light"),
                )

            // When & Then - verify no exception is thrown
            assertDoesNotThrow { processEventUseCase.execute(event) }
        }

        @Test
        @DisplayName("Should handle all event types")
        fun `should handle all event types`() {
            // Given
            val eventTypes = EventLogType.values()

            // When & Then
            eventTypes.forEach { eventType ->
                val event =
                    EventLog(
                        source = "test-source",
                        eventType = eventType,
                        timestamp = Instant.now(),
                        payload = mapOf("test" to "data"),
                    )

                assertDoesNotThrow { processEventUseCase.execute(event) }
            }
        }
    }

    @Nested
    @DisplayName("Payload Handling")
    inner class PayloadHandling {
        @Test
        @DisplayName("Should handle empty payload")
        fun `should handle empty payload`() {
            // Given
            val event =
                EventLog(
                    source = "sensor",
                    eventType = EventLogType.INFO,
                    timestamp = Instant.now(),
                    payload = emptyMap(),
                )

            // When & Then
            assertDoesNotThrow { processEventUseCase.execute(event) }
        }

        @Test
        @DisplayName("Should handle complex payload")
        fun `should handle complex payload`() {
            // Given
            val complexPayload =
                mapOf(
                    "device_info" to mapOf("name" to "Smart Thermostat", "model" to "NEST-T3"),
                    "state_change" to
                        mapOf(
                            "from" to mapOf("temperature" to 20.5),
                            "to" to mapOf("temperature" to 22.0),
                        ),
                    "metadata" to listOf("automated", "schedule_triggered"),
                )

            val event =
                EventLog(
                    source = "climate-controller",
                    eventType = EventLogType.USER_ACTION,
                    timestamp = Instant.now(),
                    payload = complexPayload,
                )

            // When & Then
            assertDoesNotThrow { processEventUseCase.execute(event) }
        }
    }
}

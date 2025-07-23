package com.homeassistant.application.usecases

import com.homeassistant.domain.enum.EventLogType
import com.homeassistant.domain.model.EventLog
import com.homeassistant.domain.port.DuplicateFilterPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Instant

@DisplayName("ProcessEventUseCase Tests")
class ProcessEventUseCaseTest {
    private lateinit var notificationPublisherUseCase: NotificationPublisherUseCase
    private lateinit var duplicateFilterPort: DuplicateFilterPort
    private lateinit var processEventUseCase: ProcessEventUseCase

    @BeforeEach
    fun setup() {
        notificationPublisherUseCase = mockk()
        duplicateFilterPort = mockk()
        processEventUseCase = ProcessEventUseCase(notificationPublisherUseCase, duplicateFilterPort)
    }

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
            every { duplicateFilterPort.isDuplicate(any()) } returns false
            every { duplicateFilterPort.recordEvent(any()) } returns Unit
            every { notificationPublisherUseCase.executeWithRetry(any(), any()) } returns true

            // When & Then - verify no exception is thrown
            assertDoesNotThrow { processEventUseCase.execute(event) }
            verify(exactly = 1) { duplicateFilterPort.isDuplicate(event) }
            verify(exactly = 1) { duplicateFilterPort.recordEvent(any()) }
            verify(exactly = 1) { notificationPublisherUseCase.executeWithRetry(any(), any()) }
        }

        @Test
        @DisplayName("Should handle all event types")
        fun `should handle all event types`() {
            // Given
            val eventTypes = EventLogType.values()
            every { duplicateFilterPort.isDuplicate(any()) } returns false
            every { duplicateFilterPort.recordEvent(any()) } returns Unit
            every { notificationPublisherUseCase.executeWithRetry(any(), any()) } returns true

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

            verify(exactly = eventTypes.size) { duplicateFilterPort.isDuplicate(any()) }
            verify(exactly = eventTypes.size) { duplicateFilterPort.recordEvent(any()) }
            verify(exactly = eventTypes.size) { notificationPublisherUseCase.executeWithRetry(any(), any()) }
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
            every { duplicateFilterPort.isDuplicate(any()) } returns false
            every { duplicateFilterPort.recordEvent(any()) } returns Unit
            every { notificationPublisherUseCase.executeWithRetry(any(), any()) } returns true

            // When & Then
            assertDoesNotThrow { processEventUseCase.execute(event) }
            verify(exactly = 1) { duplicateFilterPort.isDuplicate(event) }
            verify(exactly = 1) { duplicateFilterPort.recordEvent(any()) }
            verify(exactly = 1) { notificationPublisherUseCase.executeWithRetry(any(), any()) }
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
            every { duplicateFilterPort.isDuplicate(any()) } returns false
            every { duplicateFilterPort.recordEvent(any()) } returns Unit
            every { notificationPublisherUseCase.executeWithRetry(any(), any()) } returns true

            // When & Then
            assertDoesNotThrow { processEventUseCase.execute(event) }
            verify(exactly = 1) { duplicateFilterPort.isDuplicate(event) }
            verify(exactly = 1) { duplicateFilterPort.recordEvent(any()) }
            verify(exactly = 1) { notificationPublisherUseCase.executeWithRetry(any(), any()) }
        }
    }

    @Nested
    @DisplayName("Duplicate Event Filtering")
    inner class DuplicateEventFiltering {
        @Test
        @DisplayName("Should skip processing for duplicate events")
        fun `should skip processing for duplicate events`() {
            // Given
            val event =
                EventLog(
                    source = "temperature-sensor",
                    eventType = EventLogType.WARNING,
                    timestamp = Instant.now(),
                    payload = mapOf("temperature" to 25.0),
                )
            every { duplicateFilterPort.isDuplicate(any()) } returns true

            // When
            processEventUseCase.execute(event)

            // Then
            verify(exactly = 1) { duplicateFilterPort.isDuplicate(event) }
            verify(exactly = 0) { duplicateFilterPort.recordEvent(any()) }
            verify(exactly = 0) { notificationPublisherUseCase.executeWithRetry(any(), any()) }
        }

        @Test
        @DisplayName("Should process and record unique events")
        fun `should process and record unique events`() {
            // Given
            val event =
                EventLog(
                    source = "door-sensor",
                    eventType = EventLogType.USER_ACTION,
                    timestamp = Instant.now(),
                    payload = mapOf("door" to "opened"),
                )
            every { duplicateFilterPort.isDuplicate(any()) } returns false
            every { duplicateFilterPort.recordEvent(any()) } returns Unit
            every { notificationPublisherUseCase.executeWithRetry(any(), any()) } returns true

            // When
            processEventUseCase.execute(event)

            // Then
            verify(exactly = 1) { duplicateFilterPort.isDuplicate(event) }
            verify(exactly = 1) { duplicateFilterPort.recordEvent(any()) }
            verify(exactly = 1) { notificationPublisherUseCase.executeWithRetry(any(), any()) }
        }

        @Test
        @DisplayName("Should handle notification failure gracefully with duplicate filtering")
        fun `should handle notification failure gracefully with duplicate filtering`() {
            // Given
            val event =
                EventLog(
                    source = "error-sensor",
                    eventType = EventLogType.ERROR,
                    timestamp = Instant.now(),
                    payload = mapOf("error" to "connection_lost"),
                )
            every { duplicateFilterPort.isDuplicate(any()) } returns false
            every { duplicateFilterPort.recordEvent(any()) } returns Unit
            every { notificationPublisherUseCase.executeWithRetry(any(), any()) } returns false

            // When & Then
            assertDoesNotThrow { processEventUseCase.execute(event) }
            verify(exactly = 1) { duplicateFilterPort.isDuplicate(event) }
            verify(exactly = 1) { duplicateFilterPort.recordEvent(any()) }
            verify(exactly = 1) { notificationPublisherUseCase.executeWithRetry(any(), any()) }
        }
    }
}

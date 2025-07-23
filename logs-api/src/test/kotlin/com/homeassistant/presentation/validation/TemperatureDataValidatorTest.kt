package com.homeassistant.presentation.validation

import com.homeassistant.domain.enum.EventLogType
import com.homeassistant.presentation.dto.EventLogRequest
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.validation.Errors
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("TemperatureDataValidator Tests")
class TemperatureDataValidatorTest {
    private lateinit var validator: TemperatureDataValidator
    private lateinit var errors: Errors

    @BeforeEach
    fun setUp() {
        validator = TemperatureDataValidator()
        errors = mockk<Errors>(relaxed = true)
    }

    @Nested
    @DisplayName("Type Safety Tests")
    inner class TypeSafetyTests {
        @Test
        @DisplayName("Should support EventLogRequest class")
        fun `should support EventLogRequest class`() {
            // Given & When
            val result = validator.supports(EventLogRequest::class.java)

            // Then
            assertTrue(result)
        }

        @Test
        @DisplayName("Should not support other classes")
        fun `should not support other classes`() {
            // Given & When
            val result = validator.supports(String::class.java)

            // Then
            assertFalse(result)
        }

        @Test
        @DisplayName("Should handle non-EventLogRequest target gracefully")
        fun `should handle non-EventLogRequest target gracefully`() {
            // Given
            val invalidTarget = "not an EventLogRequest"

            // When
            validator.validate(invalidTarget, errors)

            // Then - Should not crash and not call any error methods
            verify(exactly = 0) { errors.reject(any(), any()) }
        }
    }

    @Nested
    @DisplayName("Non-Temperature Events")
    inner class NonTemperatureEventTests {
        @Test
        @DisplayName("Should not validate when alert_type is not temperature_difference")
        fun `should not validate when alert_type is not temperature_difference`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.INFO,
                    payload =
                        mapOf(
                            "alert_type" to "other_alert",
                            "message" to "Simple event",
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify(exactly = 0) { errors.reject(any(), any()) }
        }

        @Test
        @DisplayName("Should not validate when payload is null")
        fun `should not validate when payload is null`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.INFO,
                    payload = null,
                )

            // When
            validator.validate(request, errors)

            // Then
            verify(exactly = 0) { errors.reject(any(), any()) }
        }

        @Test
        @DisplayName("Should not validate when no alert_type in payload")
        fun `should not validate when no alert_type in payload`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.INFO,
                    payload = mapOf("other_data" to "value"),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify(exactly = 0) { errors.reject(any(), any()) }
        }
    }

    @Nested
    @DisplayName("Valid Temperature Events")
    inner class ValidTemperatureEventTests {
        @Test
        @DisplayName("Should accept valid temperature event with single sensor")
        fun `should accept valid temperature event with single sensor`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.WARNING,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_quarto" to
                                mapOf(
                                    "value" to 25.5,
                                    "entity_id" to "sensor.temperatura_quarto",
                                ),
                            "threshold" to 2.0,
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify(exactly = 0) { errors.reject(any(), any()) }
        }

        @Test
        @DisplayName("Should accept valid temperature event with multiple sensors")
        fun `should accept valid temperature event with multiple sensors`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.WARNING,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_quarto" to
                                mapOf(
                                    "value" to 25.5,
                                    "entity_id" to "sensor.temperatura_quarto",
                                ),
                            "temperature_sala" to
                                mapOf(
                                    "value" to 23.0,
                                    "entity_id" to "sensor.temperatura_sala",
                                ),
                            "temperature_difference" to 2.5,
                            "threshold" to 2.0,
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify(exactly = 0) { errors.reject(any(), any()) }
        }

        @Test
        @DisplayName("Should accept temperature using temperature field instead of value")
        fun `should accept temperature using temperature field instead of value`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.WARNING,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_cozinha" to
                                mapOf(
                                    "temperature" to 26.0,
                                    "entity_id" to "sensor.temperatura_cozinha",
                                ),
                            "threshold" to 1.5,
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify(exactly = 0) { errors.reject(any(), any()) }
        }

        @Test
        @DisplayName("Should accept temperature difference within tolerance")
        fun `should accept temperature difference within tolerance`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.WARNING,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_quarto" to
                                mapOf(
                                    "value" to 25.0,
                                    "entity_id" to "sensor.temperatura_quarto",
                                ),
                            "temperature_sala" to
                                mapOf(
                                    "value" to 23.0,
                                    "entity_id" to "sensor.temperatura_sala",
                                ),
                            // Actual difference is 2.0
                            "temperature_difference" to 2.0,
                            "threshold" to 1.5,
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify(exactly = 0) { errors.reject(any(), any()) }
        }
    }

    @Nested
    @DisplayName("Invalid Temperature Events")
    inner class InvalidTemperatureEventTests {
        @Test
        @DisplayName("Should reject when no temperature sensors found")
        fun `should reject when no temperature sensors found`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.ERROR,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "other_data" to "value",
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify {
                errors.reject("missing.temperature.sensors", "No temperature sensors found in payload")
            }
        }

        @Test
        @DisplayName("Should reject temperature above maximum (40°C)")
        fun `should reject temperature above maximum`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.ERROR,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_quarto" to
                                mapOf(
                                    "value" to 45.0,
                                    "entity_id" to "sensor.temperatura_quarto",
                                ),
                            "threshold" to 2.0,
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify {
                errors.reject(
                    "invalid.temperature.range",
                    "Temperature for 'temperature_quarto' (45.0°C) must be between 0.0°C and 40.0°C",
                )
            }
        }

        @Test
        @DisplayName("Should reject temperature below minimum (0°C)")
        fun `should reject temperature below minimum`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.ERROR,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_sala" to
                                mapOf(
                                    "value" to -5.0,
                                    "entity_id" to "sensor.temperatura_sala",
                                ),
                            "threshold" to 1.0,
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify {
                errors.reject(
                    "invalid.temperature.range",
                    "Temperature for 'temperature_sala' (-5.0°C) must be between 0.0°C and 40.0°C",
                )
            }
        }

        @Test
        @DisplayName("Should reject when threshold is missing")
        fun `should reject when threshold is missing`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.WARNING,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_quarto" to
                                mapOf(
                                    "value" to 25.0,
                                    "entity_id" to "sensor.temperatura_quarto",
                                ),
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify {
                errors.reject(
                    "missing.threshold",
                    "Temperature threshold is required for temperature events",
                )
            }
        }

        @Test
        @DisplayName("Should reject when threshold is zero or negative")
        fun `should reject when threshold is zero or negative`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.WARNING,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_quarto" to
                                mapOf(
                                    "value" to 25.0,
                                    "entity_id" to "sensor.temperatura_quarto",
                                ),
                            "threshold" to 0.0,
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify { errors.reject("invalid.threshold", "Temperature threshold must be greater than 0") }
        }

        @Test
        @DisplayName("Should reject when entity_id is missing")
        fun `should reject when entity_id is missing`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.WARNING,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_cozinha" to
                                mapOf(
                                    "value" to 23.5,
                                    // Missing entity_id
                                ),
                            "threshold" to 2.0,
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify {
                errors.reject(
                    "missing.entity.id",
                    "Entity ID for 'temperature_cozinha' is required (should be like 'sensor.temperatura_quarto')",
                )
            }
        }

        @Test
        @DisplayName("Should reject when entity_id is blank")
        fun `should reject when entity_id is blank`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.WARNING,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_banheiro" to
                                mapOf(
                                    "value" to 24.0,
                                    "entity_id" to "",
                                ),
                            "threshold" to 1.5,
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify {
                errors.reject(
                    "missing.entity.id",
                    "Entity ID for 'temperature_banheiro' is required (should be like 'sensor.temperatura_quarto')",
                )
            }
        }

        @Test
        @DisplayName("Should reject when temperature value is not a number")
        fun `should reject when temperature value is not a number`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.ERROR,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_quarto" to
                                mapOf(
                                    "value" to "not_a_number",
                                    "entity_id" to "sensor.temperatura_quarto",
                                ),
                            "threshold" to 2.0,
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify {
                errors.reject(
                    "invalid.temperature.value",
                    "Temperature value for 'temperature_quarto' must be a valid number",
                )
            }
        }

        @Test
        @DisplayName("Should reject when sensor data is not a map")
        fun `should reject when sensor data is not a map`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.ERROR,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_quarto" to "invalid_sensor_data",
                            "threshold" to 2.0,
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify {
                errors.reject(
                    "missing.sensor.data",
                    "Sensor data for 'temperature_quarto' is missing or invalid",
                )
            }
        }

        @Test
        @DisplayName("Should reject when temperature difference is outside tolerance")
        fun `should reject when temperature difference is outside tolerance`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.WARNING,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_quarto" to
                                mapOf(
                                    "value" to 25.0,
                                    "entity_id" to "sensor.temperatura_quarto",
                                ),
                            "temperature_sala" to
                                mapOf(
                                    "value" to 22.0,
                                    "entity_id" to "sensor.temperatura_sala",
                                ),
                            // Actual difference is 3.0, tolerance is 0.5
                            "temperature_difference" to 5.0,
                            "threshold" to 2.0,
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify {
                errors.reject(
                    "invalid.temperature.difference",
                    "Temperature difference (5.0°C) doesn't match calculated difference (3.0°C)",
                )
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    inner class EdgeCasesTests {
        @Test
        @DisplayName("Should handle sensor_id as alternative to entity_id")
        fun `should handle sensor_id as alternative to entity_id`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.INFO,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_garagem" to
                                mapOf(
                                    "value" to 15.0,
                                    "sensor_id" to "sensor.temperatura_garagem",
                                ),
                            "threshold" to 3.0,
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify(exactly = 0) { errors.reject(any(), any()) }
        }

        @Test
        @DisplayName("Should handle String temperature values that can be converted to Double")
        fun `should handle String temperature values that can be converted to Double`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.INFO,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_quarto" to
                                mapOf(
                                    "value" to 25.5,
                                    "entity_id" to "sensor.temperatura_quarto",
                                ),
                            // String threshold
                            "threshold" to "2.0",
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify(exactly = 0) { errors.reject(any(), any()) }
        }

        @Test
        @DisplayName("Should skip temperature difference validation when less than 2 sensors")
        fun `should skip temperature difference validation when less than 2 sensors`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.WARNING,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_quarto" to
                                mapOf(
                                    "value" to 25.0,
                                    "entity_id" to "sensor.temperatura_quarto",
                                ),
                            "temperature_difference" to
                                // Invalid but should be ignored
                                999.0,
                            "threshold" to 2.0,
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify(exactly = 0) { errors.reject("invalid.temperature.difference", any()) }
        }

        @Test
        @DisplayName("Should handle exception during validation gracefully")
        fun `should handle exception during validation gracefully`() {
            // Given - Create a request that will trigger validation error
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.ERROR,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_quarto" to
                                // This will cause validation error
                                "invalid_data",
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then - Should catch internal errors and report them gracefully
            verify(atLeast = 1) { errors.reject(any(), any()) }
        }

        @Test
        @DisplayName("Should handle generic temperature_* pattern for any room name")
        fun `should handle generic temperature pattern for any room name`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.INFO,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_sótão" to
                                mapOf(
                                    "value" to 18.5,
                                    "entity_id" to "sensor.temperatura_sótão",
                                ),
                            "temperature_porão" to
                                mapOf(
                                    "value" to 16.0,
                                    "entity_id" to "sensor.temperatura_porão",
                                ),
                            "threshold" to 2.0,
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify(exactly = 0) { errors.reject(any(), any()) }
        }

        @Test
        @DisplayName("Should ignore temperature_difference key when finding sensors")
        fun `should ignore temperature_difference key when finding sensors`() {
            // Given
            val request =
                EventLogRequest(
                    source = "home-assistant",
                    eventType = EventLogType.WARNING,
                    payload =
                        mapOf(
                            "alert_type" to "temperature_difference",
                            "temperature_difference" to 2.5,
                            "threshold" to 2.0,
                            // No actual temperature sensors
                        ),
                )

            // When
            validator.validate(request, errors)

            // Then
            verify {
                errors.reject("missing.temperature.sensors", "No temperature sensors found in payload")
            }
        }
    }
}

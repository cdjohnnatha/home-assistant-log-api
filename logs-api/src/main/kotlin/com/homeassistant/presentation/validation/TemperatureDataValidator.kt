package com.homeassistant.presentation.validation

import com.homeassistant.infra.extensions.logger
import com.homeassistant.presentation.dto.EventLogRequest
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator

@Component
class TemperatureDataValidator : Validator {
    companion object {
        // Realistic temperature ranges for residential use in Porto, Portugal
        private const val MIN_TEMPERATURE = 0.0 // 0°C - very cold house or sensor issue
        private const val MAX_TEMPERATURE = 40.0 // 40°C - very hot house or sensor issue
        private const val TEMPERATURE_TOLERANCE = 0.5 // Tolerance for temperature difference validation

        // Type-safe class reference to avoid reflection overhead
        private val SUPPORTED_CLASS: Class<EventLogRequest> = EventLogRequest::class.java
    }

    /**
     * More type-safe supports method with explicit type constraint
     */
    override fun supports(clazz: Class<*>): Boolean {
        // Direct class comparison (fastest)
        if (clazz == SUPPORTED_CLASS) return true

        // Check inheritance hierarchy only if needed
        return SUPPORTED_CLASS.isAssignableFrom(clazz)
    }

    override fun validate(
        target: Any,
        errors: Errors,
    ) {
        // Type-safe casting with contract validation
        val request = validateAndCast(target) ?: return

        if (isTemperatureEvent(request)) {
            logger.info("Validating temperature data for event from: ${request.source}")
            validateTemperatureData(request.payload ?: emptyMap(), errors)
        }
    }

    /**
     * Type-safe casting with clear contract validation
     */
    private fun validateAndCast(target: Any): EventLogRequest? {
        return when {
            target is EventLogRequest -> target
            else -> {
                logger.warn(
                    "TemperatureDataValidator received unexpected type: ${target::class.qualifiedName}. " +
                        "Expected: ${SUPPORTED_CLASS.simpleName}",
                )
                null
            }
        }
    }

    private fun isTemperatureEvent(request: EventLogRequest): Boolean {
        val payload = request.payload ?: return false
        return payload["alert_type"] == "temperature_difference"
    }

    private fun validateTemperatureData(
        payload: Map<String, Any>,
        errors: Errors,
    ) {
        try {
            // Find all temperature sensors in the payload
            val temperatureSensors = findTemperatureSensors(payload)

            if (temperatureSensors.isEmpty()) {
                errors.reject("missing.temperature.sensors", "No temperature sensors found in payload")
                return
            }

            // Validate each temperature sensor
            temperatureSensors.forEach { sensorKey ->
                validateSingleSensor(payload, sensorKey, errors)
            }

            // Validate threshold if present
            validateThreshold(payload, errors)

            // Validate temperature difference consistency if provided
            if (temperatureSensors.size >= 2) {
                validateTemperatureDifference(payload, temperatureSensors, errors)
            }
        } catch (e: Exception) {
            logger.error("Error during temperature validation", e)
            errors.reject("temperature.validation.error", "Error validating temperature data: ${e.message}")
        }
    }

    private fun findTemperatureSensors(payload: Map<String, Any>): List<String> {
        return payload.keys.filter { key ->
            key.startsWith("temperature_") && key != "temperature_difference"
        }
    }

    private fun validateSingleSensor(
        payload: Map<String, Any>,
        sensorKey: String,
        errors: Errors,
    ) {
        val sensorData = payload[sensorKey] as? Map<*, *>

        if (sensorData == null) {
            errors.reject("missing.sensor.data", "Sensor data for '$sensorKey' is missing or invalid")
            return
        }

        // Extract temperature value
        val temperature = extractTemperatureValue(sensorData)
        if (temperature == null) {
            errors.reject(
                "invalid.temperature.value",
                "Temperature value for '$sensorKey' must be a valid number",
            )
            return
        }

        // Validate temperature range
        if (!isValidTemperatureRange(temperature)) {
            errors.reject(
                "invalid.temperature.range",
                "Temperature for '$sensorKey' ($temperature°C) must be between $MIN_TEMPERATURE°C and $MAX_TEMPERATURE°C",
            )
        }

        // Validate entity_id presence
        val entityId = sensorData["entity_id"] as? String ?: sensorData["sensor_id"] as? String
        if (entityId.isNullOrBlank()) {
            errors.reject(
                "missing.entity.id",
                "Entity ID for '$sensorKey' is required (should be like 'sensor.temperatura_quarto')",
            )
        }
    }

    private fun validateThreshold(
        payload: Map<String, Any>,
        errors: Errors,
    ) {
        val threshold = extractNumberFromPayload(payload, "threshold")

        if (threshold == null) {
            errors.reject("missing.threshold", "Temperature threshold is required for temperature events")
            return
        }

        if (threshold <= 0) {
            errors.reject("invalid.threshold", "Temperature threshold must be greater than 0")
        }
    }

    private fun validateTemperatureDifference(
        payload: Map<String, Any>,
        temperatureSensors: List<String>,
        errors: Errors,
    ) {
        val reportedDiff = extractNumberFromPayload(payload, "temperature_difference")
        if (reportedDiff == null) {
            return // temperature_difference not provided, that's ok
        }

        // Calculate difference from first two sensors
        val firstSensorData = payload[temperatureSensors[0]] as? Map<*, *>
        val secondSensorData = payload[temperatureSensors[1]] as? Map<*, *>

        if (firstSensorData == null || secondSensorData == null) {
            return // Other validation errors will be reported
        }

        val firstTemp = extractTemperatureValue(firstSensorData)
        val secondTemp = extractTemperatureValue(secondSensorData)

        if (firstTemp != null && secondTemp != null) {
            val calculatedDiff = kotlin.math.abs(firstTemp - secondTemp)

            if (kotlin.math.abs(calculatedDiff - reportedDiff) > TEMPERATURE_TOLERANCE) {
                errors.reject(
                    "invalid.temperature.difference",
                    "Temperature difference ($reportedDiff°C) doesn't match calculated difference " +
                        "(${String.format("%.1f", calculatedDiff)}°C)",
                )
            }
        }
    }

    private fun extractTemperatureValue(sensorData: Map<*, *>): Double? {
        val value = sensorData["value"] as? Number ?: sensorData["temperature"] as? Number
        return value?.toDouble()
    }

    private fun extractNumberFromPayload(
        payload: Map<String, Any>,
        key: String,
    ): Double? {
        val value = payload[key]
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun isValidTemperatureRange(temperature: Double): Boolean {
        return temperature in MIN_TEMPERATURE..MAX_TEMPERATURE
    }
}

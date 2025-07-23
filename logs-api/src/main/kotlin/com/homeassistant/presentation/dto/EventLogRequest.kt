package com.homeassistant.presentation.dto

import com.homeassistant.domain.enum.EventLogType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class EventLogRequest(
    @field:NotBlank(message = "Source is required")
    @field:Size(min = 2, max = 100, message = "Source must be between 2 and 100 characters")
    val source: String,
    @field:NotNull(message = "Event type is required")
    val eventType: EventLogType,
    val timestamp: Instant? = null,
    val payload: Map<String, Any>? = null,
)

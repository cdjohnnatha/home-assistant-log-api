package com.homeassistant.interfaces.controller.dto

import com.homeassistant.domain.enum.EventLogType
import java.time.Instant

data class EventLogRequest(
    val source: String,
    val eventType: EventLogType,
    val timestamp: Instant? = null,
    val payload: Map<String, Any> = emptyMap()
) 
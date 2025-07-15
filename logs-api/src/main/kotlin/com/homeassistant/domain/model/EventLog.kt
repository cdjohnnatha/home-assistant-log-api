package com.homeassistant.domain.model

import com.homeassistant.domain.enum.EventLogType
import java.time.Instant

data class EventLog(
    val source: String,
    val eventType: EventLogType,
    val timestamp: Instant,
    val payload: Map<String, Any>,
)

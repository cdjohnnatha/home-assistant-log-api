package com.homeassistant.application

import com.homeassistant.domain.model.EventLog
import org.springframework.stereotype.Service

@Service
class ProcessEventUseCase {
    
    fun execute(event: EventLog) {
        // TODO: Implement event processing logic
        println("Processing event: ${event.source} - ${event.eventType} at ${event.timestamp}")
    }
} 
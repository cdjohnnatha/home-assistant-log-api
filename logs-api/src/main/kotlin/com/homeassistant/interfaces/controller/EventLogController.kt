package com.homeassistant.interfaces.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.time.Instant
import org.springframework.web.bind.annotation.RequestBody
import com.homeassistant.interfaces.controller.dto.EventLogRequest
import com.homeassistant.domain.model.EventLog
import com.homeassistant.application.usecases.ProcessEventUseCase
import org.springframework.web.bind.annotation.GetMapping

@RestController
@RequestMapping("/api/v1/events")
class EventLogController (private val processEventUseCase: ProcessEventUseCase) {

    @GetMapping("/health")
    fun healthCheck(): Map<String, Any> {
        return mapOf(
            "status" to "UP",
            "service" to "home-assistant-log-api",
            "timestamp" to Instant.now()
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun createEvent(@RequestBody request: EventLogRequest) {
        val event = EventLog(
            source = request.source,
            eventType = request.eventType,
            timestamp = request.timestamp ?: Instant.now(),
            payload = request.payload
        )

        processEventUseCase.execute(event)
    }
} 
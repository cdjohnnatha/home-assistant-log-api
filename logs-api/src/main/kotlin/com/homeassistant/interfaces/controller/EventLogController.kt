package com.homeassistant.interfaces.controller

import com.homeassistant.application.usecases.ProcessEventUseCase
import com.homeassistant.domain.model.EventLog
import com.homeassistant.interfaces.controller.dto.EventLogRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/v1/events")
class EventLogController(private val processEventUseCase: ProcessEventUseCase) {
    @GetMapping("/health")
    fun healthCheck(): Map<String, Any> {
        return mapOf(
            "status" to "UP",
            "service" to "home-assistant-log-api",
            "timestamp" to Instant.now(),
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun createEvent(
        @RequestBody request: EventLogRequest,
    ) {
        val event =
            EventLog(
                source = request.source,
                eventType = request.eventType,
                timestamp = request.timestamp ?: Instant.now(),
                payload = request.payload,
            )

        processEventUseCase.execute(event)
    }
}

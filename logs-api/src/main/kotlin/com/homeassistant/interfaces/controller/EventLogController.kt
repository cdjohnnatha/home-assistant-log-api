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
import com.homeassistant.application.ProcessEventUseCase

@RestController
@RequestMapping("/api/v1/events")
class EventLogController (private val processEventUseCase: ProcessEventUseCase) {

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
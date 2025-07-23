package com.homeassistant.presentation.controller

import com.homeassistant.application.usecases.ProcessEventUseCase
import com.homeassistant.domain.model.EventLog
import com.homeassistant.presentation.dto.EventLogRequest
import com.homeassistant.presentation.validation.TemperatureDataValidator
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/v1/events")
@Validated
class EventLogController(
    private val processEventUseCase: ProcessEventUseCase,
    private val temperatureDataValidator: TemperatureDataValidator,
) {
    @InitBinder
    fun initBinder(binder: WebDataBinder) {
        binder.addValidators(temperatureDataValidator)
    }

    @GetMapping("/health")
    @ResponseBody
    fun healthCheck(): String {
        return "Ok"
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun createEvent(
        @Valid @RequestBody request: EventLogRequest,
    ) {
        val event =
            EventLog(
                source = request.source,
                eventType = request.eventType,
                timestamp = request.timestamp ?: Instant.now(),
                payload = request.payload ?: emptyMap(),
            )

        processEventUseCase.execute(event)
    }
}

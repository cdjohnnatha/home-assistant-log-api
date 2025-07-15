package com.homeassistant.application.usecases

import com.homeassistant.domain.model.EventLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProcessEventUseCase {
    private val logger = LoggerFactory.getLogger(ProcessEventUseCase::class.java)

    fun execute(event: EventLog) {
        logger.info("event processed: $event")
    }
}

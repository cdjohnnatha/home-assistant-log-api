package com.homeassistant.application.usecases

import com.homeassistant.domain.model.EventLog
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class ProcessEventUseCase {
    private val logger = LoggerFactory.getLogger(ProcessEventUseCase::class.java)
    
    fun execute(event: EventLog) {
        logger.info("event processed: $event")
    }
} 
package com.homeassistant.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.homeassistant.TestConfiguration
import com.homeassistant.application.usecases.ProcessEventUseCase
import com.homeassistant.domain.enum.EventLogType
import com.homeassistant.logsapi.LogsApiApplication
import com.homeassistant.presentation.dto.EventLogRequest
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest(classes = [LogsApiApplication::class, TestConfiguration::class])
@AutoConfigureMockMvc
@ActiveProfiles("test") // Use test profile with mock AWS config
@DisplayName("EventLogController Tests")
class EventLogControllerTest {
    @Autowired private lateinit var mockMvc: MockMvc

    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean(relaxed = true)
    private lateinit var processEventUseCase: ProcessEventUseCase

    @Test
    @DisplayName("Should return health status successfully")
    fun `should return health status successfully`() {
        mockMvc.perform(get("/api/v1/events/health"))
            .andExpect(status().isOk())
            .andExpect(content().string("Ok"))
    }

    @Test
    @DisplayName("Should process event log successfully")
    fun `should process event log successfully`() {
        // Given
        val event =
            EventLogRequest(
                source = "test",
                eventType = EventLogType.USER_ACTION,
                timestamp = Instant.now(),
                payload = emptyMap(),
            )

        // When & Then
        mockMvc.perform(
            post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)),
        )
            .andExpect(status().isAccepted())
    }
}

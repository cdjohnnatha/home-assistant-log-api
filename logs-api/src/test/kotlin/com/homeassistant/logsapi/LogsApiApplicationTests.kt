package com.homeassistant.logsapi

import com.homeassistant.TestConfiguration
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [LogsApiApplication::class, TestConfiguration::class])
@ActiveProfiles("test")
class LogsApiApplicationTests {
    @Test
    fun contextLoads() {
    }
}

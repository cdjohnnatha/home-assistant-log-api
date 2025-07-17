package com.homeassistant

import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.services.sns.SnsClient

@TestConfiguration
@Profile("test")
class TestConfiguration {
    @Bean
    @Primary
    fun mockSnsClient(): SnsClient {
        return mockk<SnsClient>(relaxed = true)
    }
}

package com.homeassistant.infra.config

import com.homeassistant.infra.extensions.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient

@Configuration
@Profile("!test")
class SnsConfig {
    @Bean
    fun snsClient(): SnsClient {
        val regionName =
            System.getenv("AWS_REGION")
                ?: throw IllegalStateException("AWS_REGION environment variable is required")

        logger.info("Set SNS Client for region: $regionName using IAM role")

        // Use IAM role attached to EC2 instance (best practice)
        // No need for access keys when running on EC2 with IAM role
        return SnsClient.builder()
            .region(Region.of(regionName))
            .build()
    }
}

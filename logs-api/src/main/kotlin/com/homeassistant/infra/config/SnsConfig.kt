package com.homeassistant.infra.config

import com.homeassistant.infra.extensions.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient

@Configuration
@Profile("!test")
class SnsConfig {
    @Bean
    fun snsClient(): SnsClient {
        val accessKey =
            System.getenv("AWS_ACCESS_KEY")
                ?: throw IllegalStateException("AWS_ACCESS_KEY environment variable is required")
        val secretKey =
            System.getenv("AWS_SECRET_KEY")
                ?: throw IllegalStateException("AWS_SECRET_KEY environment variable is required")
        val regionName =
            System.getenv("AWS_REGION")
                ?: throw IllegalStateException("AWS_REGION environment variable is required")

        logger.info("Set SNS Client for region: $regionName")

        val credentials = AwsBasicCredentials.create(accessKey, secretKey)
        val region = Region.of(regionName)

        return SnsClient.builder()
            .region(region)
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }
}

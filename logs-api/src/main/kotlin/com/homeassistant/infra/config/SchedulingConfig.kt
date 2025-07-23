package com.homeassistant.infra.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Configuration to enable Spring's @Scheduled annotation
 * Required for automatic cleanup of duplicate filter cache
 */
@Configuration
@EnableScheduling
class SchedulingConfig

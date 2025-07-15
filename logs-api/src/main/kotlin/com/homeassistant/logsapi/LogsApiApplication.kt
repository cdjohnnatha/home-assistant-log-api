package com.homeassistant.logsapi

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@SpringBootApplication
@ComponentScan(basePackages = ["com.homeassistant"])
class LogsApiApplication

@Component
class EnvironmentLogger(
    private val environment: Environment,
    @Value("\${server.port:8080}") private val serverPort: String,
    @Value("\${logging.level.com.homeassistant:INFO}") private val logLevel: String,
) : CommandLineRunner {
    private val logger = LoggerFactory.getLogger(EnvironmentLogger::class.java)

    override fun run(vararg args: String?) {
        val activeProfiles = environment.activeProfiles.joinToString(", ")

        logger.info("=== 🚀 LOGS API STARTING ===")
        logger.info("📍 Active profile: [$activeProfiles]")
        logger.info("🌐 Server port: $serverPort")
        logger.info("📊 Log level: $logLevel")
        logger.info("================================")
    }
}

fun main(args: Array<String>) {
    runApplication<LogsApiApplication>(*args)
}

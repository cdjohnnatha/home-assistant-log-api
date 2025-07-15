package com.homeassistant.logs_api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.homeassistant"])
class LogsApiApplication

fun main(args: Array<String>) {
	runApplication<LogsApiApplication>(*args)
} 
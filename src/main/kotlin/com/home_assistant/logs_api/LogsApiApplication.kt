package com.home_assistant.logs_api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LogsApiApplication

fun main(args: Array<String>) {
	runApplication<LogsApiApplication>(*args)
}

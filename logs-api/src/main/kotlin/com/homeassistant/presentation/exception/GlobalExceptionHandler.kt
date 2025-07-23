package com.homeassistant.presentation.exception

import com.homeassistant.infra.extensions.logger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.time.Instant

@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }

        logger.warn("Validation failed: {}", errors.joinToString(", "))

        return ResponseEntity.badRequest()
            .body(
                ErrorResponse(
                    status = 400,
                    error = "Bad Request",
                    message = "Validation failed: ${errors.joinToString(", ")}",
                    path = "/api/v1/events",
                ),
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericError(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", ex)

        return ResponseEntity.internalServerError()
            .body(
                ErrorResponse(
                    status = 500,
                    error = "Internal Server Error",
                    message = "An unexpected error occurred",
                    path = null,
                ),
            )
    }
}

data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String?,
)

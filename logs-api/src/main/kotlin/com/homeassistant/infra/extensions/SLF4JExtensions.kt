package com.homeassistant.infra.extensions

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Extension property para SLF4J - uso automático em qualquer classe
 * 
 * Uso:
 * ```kotlin
 * import com.homeassistant.infra.extensions.logger
 * 
 * class MyClass {
 *     fun method() {
 *         logger.info("Log message")
 *         logger.error("Error", exception)
 *     }
 * }
 * ```
 */
val <T : Any> T.logger: Logger
    get() = LoggerFactory.getLogger(this::class.java) 
package com.homeassistant.domain.port

interface NotificationPublisherPort {
  fun publish(message: String): Boolean
}

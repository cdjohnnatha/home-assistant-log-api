package com.homeassistant.infra.aws

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import software.amazon.awssdk.services.sns.model.SnsException

@DisplayName("NotificationPublisherAdapter Tests")
class NotificationPublisherAdapterTest {

    private lateinit var snsClient: SnsClient
    private lateinit var adapter: NotificationPublisherAdapter
    private lateinit var testMessage: String
    private lateinit var testTopicArn: String
    private lateinit var testMessageId: String

    @BeforeEach
    fun setup() {
        snsClient = mockk()
        adapter = NotificationPublisherAdapter(snsClient)
        testMessage = "Test notification message"
        testTopicArn = "arn:aws:sns:us-east-1:123456789012:test-topic"
        testMessageId = "12345-67890-abcdef"
        
        // Set a valid topic ARN using reflection
        ReflectionTestUtils.setField(adapter, "topicArn", testTopicArn)
    }

    @Nested
    @DisplayName("Successful Publishing")
    inner class SuccessfulPublishing {

        @Test
        @DisplayName("Should publish message successfully and return true")
        fun `should publish message successfully and return true`() {
            // Given
            val mockResponse = mockk<PublishResponse>()
            every { mockResponse.messageId() } returns testMessageId
            every { snsClient.publish(any<PublishRequest>()) } returns mockResponse

            // When
            val result = adapter.publish(testMessage)

            // Then
            assertTrue(result)
            
            // Verify the correct parameters were passed to SNS
            val publishRequestSlot = slot<PublishRequest>()
            verify(exactly = 1) { snsClient.publish(capture(publishRequestSlot)) }
            
            val capturedRequest = publishRequestSlot.captured
            assertTrue(capturedRequest.topicArn() == testTopicArn)
            assertTrue(capturedRequest.message() == testMessage)
            assertTrue(capturedRequest.subject() == "Home-Assistant-Event")
        }

        @Test
        @DisplayName("Should handle empty message")
        fun `should handle empty message`() {
            // Given
            val emptyMessage = ""
            val mockResponse = mockk<PublishResponse>()
            every { mockResponse.messageId() } returns "empty-msg-123"
            every { snsClient.publish(any<PublishRequest>()) } returns mockResponse

            // When
            val result = adapter.publish(emptyMessage)

            // Then
            assertTrue(result)
            verify(exactly = 1) { snsClient.publish(any<PublishRequest>()) }
        }
    }

    @Nested
    @DisplayName("Configuration Errors")
    inner class ConfigurationErrors {

        @Test
        @DisplayName("Should return false when topic ARN is null")
        fun `should return false when topic ARN is null`() {
            // Given
            ReflectionTestUtils.setField(adapter, "topicArn", null)

            // When
            val result = adapter.publish(testMessage)

            // Then
            assertFalse(result)
            verify(exactly = 0) { snsClient.publish(any<PublishRequest>()) }
        }

        @Test
        @DisplayName("Should return false when topic ARN is empty")
        fun `should return false when topic ARN is empty`() {
            // Given
            ReflectionTestUtils.setField(adapter, "topicArn", "")

            // When
            val result = adapter.publish(testMessage)

            // Then
            assertFalse(result)
            verify(exactly = 0) { snsClient.publish(any<PublishRequest>()) }
        }
    }

    @Nested
    @DisplayName("SNS Exceptions")
    inner class SnsExceptions {

        @Test
        @DisplayName("Should return false when SNS client throws exception")
        fun `should return false when SNS client throws exception`() {
            // Given
            every { snsClient.publish(any<PublishRequest>()) } throws SnsException.builder()
                .message("Access denied")
                .build()

            // When
            val result = adapter.publish(testMessage)

            // Then
            assertFalse(result)
            verify(exactly = 1) { snsClient.publish(any<PublishRequest>()) }
        }

        @Test
        @DisplayName("Should return false when unexpected exception occurs")
        fun `should return false when unexpected exception occurs`() {
            // Given
            every { snsClient.publish(any<PublishRequest>()) } throws RuntimeException("Network error")

            // When
            val result = adapter.publish(testMessage)

            // Then
            assertFalse(result)
            verify(exactly = 1) { snsClient.publish(any<PublishRequest>()) }
        }
    }
} 
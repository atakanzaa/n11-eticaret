package com.smartcommerce.common.events.config;

import com.smartcommerce.common.events.Topics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Auto-configures a uniform Kafka @KafkaListener error handler across all services:
 *
 *  - Exponential backoff: 500ms initial, x2.0 multiplier, capped at 30s, max 5 retries (~30s total)
 *  - After retries exhausted, the failed record is published to "{topic}.dlq" via the
 *    same KafkaTemplate (DeadLetterPublishingRecoverer)
 *  - Non-retryable exceptions (validation/illegal-state) skip retry and go straight to DLQ
 *
 * Kicks in only when spring-kafka is on the classpath, so producer-only services
 * are unaffected.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.kafka.annotation.KafkaListener")
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
            (ConsumerRecord<?, ?> record, Exception ex) ->
                new TopicPartition(Topics.dlqOf(record.topic()), record.partition()));

        var backOff = new ExponentialBackOff(500L, 2.0);
        backOff.setMaxElapsedTime(30_000L);
        backOff.setMaxInterval(8_000L);

        var handler = new DefaultErrorHandler(recoverer, backOff);
        // Don't waste retries on programmer errors — fail fast to DLQ.
        handler.addNotRetryableExceptions(IllegalArgumentException.class, IllegalStateException.class);
        return handler;
    }
}

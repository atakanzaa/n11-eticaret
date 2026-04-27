package com.smartcommerce.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.*;
import org.springframework.context.annotation.*;

@Configuration
public class RabbitMQConfig {
    public static final String EMAIL_EXCHANGE = "notifications.email";
    public static final String EMAIL_QUEUE = "notifications.email.send";
    public static final String EMAIL_DLQ = "notifications.email.send.dlq";
    public static final String EMAIL_ROUTING_KEY = "email.send";

    @Bean
    DirectExchange emailExchange() {
        return ExchangeBuilder.directExchange(EMAIL_EXCHANGE).durable(true).build();
    }

    @Bean
    Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", EMAIL_DLQ)
            .build();
    }

    @Bean
    Queue emailDlq() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(emailExchange()).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

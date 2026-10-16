package com.example.Backend.config;

import com.example.Backend.messaging.RabbitTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitMqConfig {

    @Bean
    Queue productQueue() {
        return QueueBuilder.durable(RabbitTopology.PRODUCT_QUEUE)
                .deadLetterExchange(RabbitTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(RabbitTopology.DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue productDeadLetterQueue() {
        return QueueBuilder.durable(RabbitTopology.DEAD_LETTER_QUEUE).build();
    }

    @Bean
    TopicExchange productExchange() {
        return new TopicExchange(RabbitTopology.PRODUCT_EXCHANGE);
    }

    @Bean
    DirectExchange productDeadLetterExchange() {
        return new DirectExchange(RabbitTopology.DEAD_LETTER_EXCHANGE);
    }

    @Bean
    Binding productBinding(
            @Qualifier("productQueue") Queue productQueue,
            TopicExchange productExchange) {
        return BindingBuilder.bind(productQueue)
                .to(productExchange)
                .with(RabbitTopology.PRODUCT_ROUTING_KEY);
    }

    @Bean
    Binding productDeadLetterBinding(
            @Qualifier("productDeadLetterQueue") Queue productDeadLetterQueue,
            DirectExchange productDeadLetterExchange) {
        return BindingBuilder.bind(productDeadLetterQueue)
                .to(productDeadLetterExchange)
                .with(RabbitTopology.DEAD_LETTER_ROUTING_KEY);
    }
}

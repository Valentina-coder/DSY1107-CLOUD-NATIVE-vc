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
    Queue inventoryQueue() {
        return QueueBuilder.durable(RabbitTopology.INVENTORY_QUEUE)
                .deadLetterExchange(RabbitTopology.INVENTORY_DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(RabbitTopology.INVENTORY_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue inventoryDeadLetterQueue() {
        return QueueBuilder.durable(RabbitTopology.INVENTORY_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable(RabbitTopology.NOTIFICATION_QUEUE)
                .deadLetterExchange(RabbitTopology.NOTIFICATION_DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(RabbitTopology.NOTIFICATION_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue notificationDeadLetterQueue() {
        return QueueBuilder.durable(RabbitTopology.NOTIFICATION_DEAD_LETTER_QUEUE).build();
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
    DirectExchange inventoryDeadLetterExchange() {
        return new DirectExchange(RabbitTopology.INVENTORY_DEAD_LETTER_EXCHANGE);
    }

    @Bean
    DirectExchange notificationDeadLetterExchange() {
        return new DirectExchange(RabbitTopology.NOTIFICATION_DEAD_LETTER_EXCHANGE);
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

    @Bean
    Binding inventoryBinding(
            @Qualifier("inventoryQueue") Queue inventoryQueue,
            TopicExchange productExchange) {
        return BindingBuilder.bind(inventoryQueue)
                .to(productExchange)
                .with(RabbitTopology.INVENTORY_ROUTING_KEY);
    }

    @Bean
    Binding inventoryDeadLetterBinding(
            @Qualifier("inventoryDeadLetterQueue") Queue inventoryDeadLetterQueue,
            @Qualifier("inventoryDeadLetterExchange") DirectExchange inventoryDeadLetterExchange) {
        return BindingBuilder.bind(inventoryDeadLetterQueue)
                .to(inventoryDeadLetterExchange)
                .with(RabbitTopology.INVENTORY_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    Binding notificationBinding(
            @Qualifier("notificationQueue") Queue notificationQueue,
            TopicExchange productExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(productExchange)
                .with(RabbitTopology.NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    Binding notificationDeadLetterBinding(
            @Qualifier("notificationDeadLetterQueue") Queue notificationDeadLetterQueue,
            @Qualifier("notificationDeadLetterExchange") DirectExchange notificationDeadLetterExchange) {
        return BindingBuilder.bind(notificationDeadLetterQueue)
                .to(notificationDeadLetterExchange)
                .with(RabbitTopology.NOTIFICATION_DEAD_LETTER_ROUTING_KEY);
    }
}

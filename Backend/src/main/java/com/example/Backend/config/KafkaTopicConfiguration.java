package com.example.Backend.config;

import com.example.Backend.messaging.KafkaTopology;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfiguration {

    @Bean
    NewTopic inventoryTopic() {
        return TopicBuilder.name(KafkaTopology.INVENTORY_TOPIC)
                .partitions(3)
                .replicas((short) 3)
                .build();
    }

    @Bean
    NewTopic notificationTopic() {
        return TopicBuilder.name(KafkaTopology.NOTIFICATION_TOPIC)
                .partitions(3)
                .replicas((short) 3)
                .build();
    }
}

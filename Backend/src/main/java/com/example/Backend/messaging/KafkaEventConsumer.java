package com.example.Backend.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final InventoryEventHandler inventoryEventHandler;
    private final NotificationRequestHandler notificationRequestHandler;

    public KafkaEventConsumer(
            ObjectMapper objectMapper,
            InventoryEventHandler inventoryEventHandler,
            NotificationRequestHandler notificationRequestHandler) {
        this.objectMapper = objectMapper;
        this.inventoryEventHandler = inventoryEventHandler;
        this.notificationRequestHandler = notificationRequestHandler;
    }

    @KafkaListener(topics = KafkaTopology.INVENTORY_TOPIC)
    public void consumirActualizacionInventario(ConsumerRecord<String, String> record) {
        try {
            InventoryUpdatedEvent event = objectMapper.readValue(record.value(), InventoryUpdatedEvent.class);
            inventoryEventHandler.actualizarStock(event);
            logger.info("Evento Kafka de inventario procesado: topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset());
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            logger.error("Evento Kafka de inventario inválido; topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset(), exception);
            throw new IllegalArgumentException("Evento Kafka de inventario inválido", exception);
        }
    }

    @KafkaListener(topics = KafkaTopology.NOTIFICATION_TOPIC)
    public void consumirSolicitudNotificacion(ConsumerRecord<String, String> record) {
        try {
            notificationRequestHandler.registrar(record.value());
            logger.info("Evento Kafka de notificación procesado: topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset());
        } catch (IllegalArgumentException exception) {
            logger.error("Evento Kafka de notificación inválido; topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset(), exception);
            throw new IllegalArgumentException("Evento Kafka de notificación inválido", exception);
        }
    }
}

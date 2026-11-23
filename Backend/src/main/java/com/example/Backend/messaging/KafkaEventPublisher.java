package com.example.Backend.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publicarActualizacionInventario(InventoryUpdatedEvent event) {
        publicar(KafkaTopology.INVENTORY_TOPIC, event.productoId().toString(), event);
        logger.info("Evento de inventario publicado en Kafka: productoId={}", event.productoId());
    }

    public void publicarSolicitudNotificacion(NotificationRequestedEvent event) {
        String key = event.productoId() == null ? event.tipo() : event.productoId().toString();
        publicar(KafkaTopology.NOTIFICATION_TOPIC, key, event);
        logger.info("Solicitud de notificación publicada en Kafka: tipo={}", event.tipo());
    }

    private void publicar(String topic, String key, Object event) {
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el evento Kafka", exception);
        }

        try {
            kafkaTemplate.send(new ProducerRecord<>(topic, key, payload))
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaPublishException("Se interrumpió la publicación del evento Kafka", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new KafkaPublishException("No se pudo confirmar la publicación del evento Kafka", exception);
        }
    }
}

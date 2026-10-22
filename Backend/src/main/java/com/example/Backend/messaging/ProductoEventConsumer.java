package com.example.Backend.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ProductoEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ProductoEventConsumer.class);

    private final ObjectMapper objectMapper;

    public ProductoEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitTopology.PRODUCT_QUEUE)
    public void consumirProductoCreado(String payload) {
        final ProductoCreadoEvent event;
        try {
            event = objectMapper.readValue(payload, ProductoCreadoEvent.class);
        } catch (JsonProcessingException exception) {
            logger.error("Evento inválido; se rechazará hacia la DLQ. payload={}",
                    limitarPayload(payload), exception);
            throw new AmqpRejectAndDontRequeueException("Evento de producto inválido", exception);
        }

        if (event.productoId() == null || event.nombre() == null || event.nombre().isBlank()) {
            logger.error("Evento incompleto; se rechazará hacia la DLQ. payload={}", limitarPayload(payload));
            throw new AmqpRejectAndDontRequeueException("El evento de producto no contiene los datos requeridos");
        }

        logger.info("Evento de producto procesado: productoId={}, nombre={}",
                event.productoId(), event.nombre());
    }

    @RabbitListener(queues = RabbitTopology.INVENTORY_QUEUE)
    public void consumirEventoInventario(String payload) {
        validarEventoOperativo(payload, "inventario");
    }

    @RabbitListener(queues = RabbitTopology.NOTIFICATION_QUEUE)
    public void consumirEventoNotificacion(String payload) {
        validarEventoOperativo(payload, "notificaciones");
    }

    private void validarEventoOperativo(String payload, String tipo) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            if (event == null || !event.isObject() || event.isEmpty()) {
                throw new AmqpRejectAndDontRequeueException(
                        "El evento de " + tipo + " debe ser un objeto JSON no vacío");
            }
            logger.info("Evento de {} validado: {}", tipo, limitarPayload(payload));
        } catch (JsonProcessingException exception) {
            logger.error("Evento de {} inválido; se rechazará hacia su DLQ. payload={}",
                    tipo, limitarPayload(payload), exception);
            throw new AmqpRejectAndDontRequeueException(
                    "Evento de " + tipo + " inválido", exception);
        }
    }

    private String limitarPayload(String payload) {
        if (payload == null || payload.length() <= 1000) {
            return payload;
        }
        return payload.substring(0, 1000) + "...";
    }
}

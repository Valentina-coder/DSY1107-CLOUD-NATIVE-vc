package com.example.Backend.messaging;

import com.example.Backend.model.Producto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductoEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ProductoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public ProductoEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publicarProductoCreado(Producto producto) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new ProductoCreadoEvent(producto.getId(), producto.getNombre()));
            rabbitTemplate.convertAndSend(
                    RabbitTopology.PRODUCT_EXCHANGE,
                    RabbitTopology.PRODUCT_ROUTING_KEY,
                    payload);
            logger.info("Evento de producto publicado: productoId={}", producto.getId());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el evento de producto", exception);
        }
    }
}

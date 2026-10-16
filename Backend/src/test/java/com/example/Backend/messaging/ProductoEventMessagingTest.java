package com.example.Backend.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.example.Backend.model.Producto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class ProductoEventMessagingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publishesCreatedProductAsJsonToTheProductExchange() {
        RabbitTemplate rabbitTemplate = org.mockito.Mockito.mock(RabbitTemplate.class);
        ProductoEventPublisher publisher = new ProductoEventPublisher(rabbitTemplate, objectMapper);
        Producto producto = new Producto();
        producto.setId(42L);
        producto.setNombre("Teclado");

        publisher.publicarProductoCreado(producto);

        verify(rabbitTemplate).convertAndSend(
                RabbitTopology.PRODUCT_EXCHANGE,
                RabbitTopology.PRODUCT_ROUTING_KEY,
                "{\"productoId\":42,\"nombre\":\"Teclado\"}");
    }

    @Test
    void rejectsMalformedMessagesSoRabbitCanRouteThemToTheDeadLetterQueue() {
        ProductoEventConsumer consumer = new ProductoEventConsumer(objectMapper);

        assertThatThrownBy(() -> consumer.consumirProductoCreado("{invalid-json"))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("Evento de producto inválido");
    }

    @Test
    void acceptsAValidProductEvent() {
        ProductoEventConsumer consumer = new ProductoEventConsumer(objectMapper);

        consumer.consumirProductoCreado("{\"productoId\":42,\"nombre\":\"Teclado\"}");
    }

    @Test
    void rejectsEventsWithoutRequiredProductData() {
        ProductoEventConsumer consumer = new ProductoEventConsumer(objectMapper);

        assertThatThrownBy(() -> consumer.consumirProductoCreado("{\"productoId\":null,\"nombre\":\" \"}"))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("datos requeridos");
    }
}

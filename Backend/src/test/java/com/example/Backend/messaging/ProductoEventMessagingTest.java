package com.example.Backend.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.Backend.model.NotificationRequest;
import com.example.Backend.model.Producto;
import com.example.Backend.repository.NotificationRequestRepository;
import com.example.Backend.repository.ProductoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class ProductoEventMessagingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProductoRepository productoRepository = org.mockito.Mockito.mock(ProductoRepository.class);
    private final KafkaEventPublisher kafkaEventPublisher =
            org.mockito.Mockito.mock(KafkaEventPublisher.class);
    private final NotificationRequestRepository notificationRequestRepository =
            org.mockito.Mockito.mock(NotificationRequestRepository.class);

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
        ProductoEventConsumer consumer = consumer();

        assertThatThrownBy(() -> consumer.consumirProductoCreado("{invalid-json"))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("Evento de producto inválido");
    }

    @Test
    void acceptsAValidProductEvent() {
        consumer().consumirProductoCreado("{\"productoId\":42,\"nombre\":\"Teclado\"}");
    }

    @Test
    void rejectsEventsWithoutRequiredProductData() {
        assertThatThrownBy(() -> consumer().consumirProductoCreado(
                "{\"productoId\":null,\"nombre\":\" \"}"))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("datos requeridos");
    }

    @Test
    void rejectsMalformedInventoryEventsSoRabbitCanRouteThemToTheirDeadLetterQueue() {
        assertThatThrownBy(() -> consumer().consumirEventoInventario("{invalid-json"))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("inventario inválido");
    }

    @Test
    void updatesProductStockFromInventoryEvents() {
        Producto producto = new Producto();
        producto.setId(42L);
        when(productoRepository.findById(42L)).thenReturn(Optional.of(producto));

        consumer().consumirEventoInventario("{\"productoId\":42,\"stock\":17}");

        verify(productoRepository).save(producto);
        assertThat(producto.getStock()).isEqualTo(17);
    }

    @Test
    void rejectsInventoryEventsWithNegativeStock() {
        assertThatThrownBy(() -> consumer().consumirEventoInventario(
                "{\"productoId\":42,\"stock\":-1}"))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("no negativos");
    }

    @Test
    void persistsNotificationRequests() {
        when(notificationRequestRepository.save(org.mockito.ArgumentMatchers.any(NotificationRequest.class)))
                .thenAnswer(invocation -> {
                    NotificationRequest request = invocation.getArgument(0);
                    ReflectionTestUtils.setField(request, "id", 1L);
                    return request;
                });

        consumer().consumirEventoNotificacion("{\"tipo\":\"stock-bajo\",\"productoId\":42}");

        org.mockito.ArgumentCaptor<NotificationRequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationRequestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getType()).isEqualTo("stock-bajo");
        assertThat(requestCaptor.getValue().getPayload()).contains("\"productoId\":42");
        assertThat(requestCaptor.getValue().getReceivedAt()).isNotNull();
    }

    @Test
    void rejectsNotificationRequestsWithoutType() {
        assertThatThrownBy(() -> consumer().consumirEventoNotificacion("{\"productoId\":42}"))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("requiere un tipo");
    }

    private ProductoEventConsumer consumer() {
        return new ProductoEventConsumer(
                objectMapper,
                new InventoryEventHandler(productoRepository, kafkaEventPublisher),
                new NotificationRequestHandler(objectMapper, notificationRequestRepository));
    }
}

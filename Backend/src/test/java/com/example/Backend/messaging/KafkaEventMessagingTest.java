package com.example.Backend.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.example.Backend.model.NotificationRequest;
import com.example.Backend.model.Producto;
import com.example.Backend.repository.NotificationRequestRepository;
import com.example.Backend.repository.ProductoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class KafkaEventMessagingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProductoRepository productoRepository = org.mockito.Mockito.mock(ProductoRepository.class);
    private final KafkaEventPublisher notificationPublisher =
            org.mockito.Mockito.mock(KafkaEventPublisher.class);
    private final NotificationRequestRepository notificationRequestRepository =
            org.mockito.Mockito.mock(NotificationRequestRepository.class);

    @Test
    void publishesInventoryUpdatesToKafka() {
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplate();
        KafkaEventPublisher publisher = new KafkaEventPublisher(kafkaTemplate, objectMapper);

        publisher.publicarActualizacionInventario(new InventoryUpdatedEvent(42L, 17));

        org.mockito.ArgumentCaptor<ProducerRecord> recordCaptor =
                org.mockito.ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(recordCaptor.capture());
        assertThat(recordCaptor.getValue().topic()).isEqualTo(KafkaTopology.INVENTORY_TOPIC);
        assertThat(recordCaptor.getValue().key()).isEqualTo("42");
        assertThat(recordCaptor.getValue().value()).isEqualTo("{\"productoId\":42,\"stock\":17}");
    }

    @Test
    void publishesNotificationRequestsToKafka() {
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplate();
        KafkaEventPublisher publisher = new KafkaEventPublisher(kafkaTemplate, objectMapper);

        publisher.publicarSolicitudNotificacion(
                new NotificationRequestedEvent("stock-bajo", 42L, "Revisar disponibilidad"));

        org.mockito.ArgumentCaptor<ProducerRecord> recordCaptor =
                org.mockito.ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(recordCaptor.capture());
        assertThat(recordCaptor.getValue().topic()).isEqualTo(KafkaTopology.NOTIFICATION_TOPIC);
        assertThat(recordCaptor.getValue().value())
                .isEqualTo("{\"tipo\":\"stock-bajo\",\"productoId\":42,\"mensaje\":\"Revisar disponibilidad\"}");
    }

    @Test
    void kafkaConsumersApplyInventoryAndNotificationBusinessHandlers() {
        KafkaEventConsumer consumer = consumer();
        Producto producto = new Producto();
        producto.setId(42L);
        when(productoRepository.findById(42L)).thenReturn(Optional.of(producto));
        when(notificationRequestRepository.save(org.mockito.ArgumentMatchers.any(NotificationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        consumer.consumirActualizacionInventario(new ConsumerRecord<>(
                KafkaTopology.INVENTORY_TOPIC, 0, 0L, "42", "{\"productoId\":42,\"stock\":17}"));
        consumer.consumirSolicitudNotificacion(new ConsumerRecord<>(
                KafkaTopology.NOTIFICATION_TOPIC, 0, 0L, "42",
                "{\"tipo\":\"stock-bajo\",\"productoId\":42}"));

        assertThat(producto.getStock()).isEqualTo(17);
        verify(productoRepository).save(producto);
        verify(notificationRequestRepository)
                .save(org.mockito.ArgumentMatchers.any(NotificationRequest.class));
    }

    @Test
    void rejectsMalformedKafkaInventoryMessages() {
        assertThatThrownBy(() -> consumer().consumirActualizacionInventario(new ConsumerRecord<>(
                KafkaTopology.INVENTORY_TOPIC, 0, 0L, "42", "{invalid-json")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void publishesOneCriticalStockAlertPerThresholdCrossing() {
        Producto producto = new Producto();
        producto.setId(42L);
        producto.setNombre("Teclado");
        producto.setStock(10);
        producto.setStockMinimo(5);
        when(productoRepository.findById(42L)).thenReturn(Optional.of(producto));
        InventoryEventHandler handler = new InventoryEventHandler(productoRepository, notificationPublisher);

        handler.actualizarStock(new InventoryUpdatedEvent(42L, 5));
        handler.actualizarStock(new InventoryUpdatedEvent(42L, 3));
        handler.actualizarStock(new InventoryUpdatedEvent(42L, 8));
        handler.actualizarStock(new InventoryUpdatedEvent(42L, 5));

        org.mockito.ArgumentCaptor<NotificationRequestedEvent> eventCaptor =
                org.mockito.ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(notificationPublisher, times(2)).publicarSolicitudNotificacion(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .allSatisfy(event -> {
                    assertThat(event.tipo()).isEqualTo("stock-critico");
                    assertThat(event.productoId()).isEqualTo(42L);
                    assertThat(event.mensaje()).contains("stock actual 5", "mínimo 5");
                });
        assertThat(producto.getStock()).isEqualTo(5);
    }

    @Test
    void reportsWhenKafkaDoesNotConfirmTheMessage() {
        KafkaTemplate<String, String> kafkaTemplate = org.mockito.Mockito.mock(KafkaTemplate.class);
        when(kafkaTemplate.send(org.mockito.ArgumentMatchers.<ProducerRecord<String, String>>any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));
        KafkaEventPublisher publisher = new KafkaEventPublisher(kafkaTemplate, objectMapper);

        assertThatThrownBy(() -> publisher.publicarActualizacionInventario(
                new InventoryUpdatedEvent(42L, 17)))
                .isInstanceOf(KafkaPublishException.class);
    }

    private KafkaTemplate<String, String> kafkaTemplate() {
        KafkaTemplate<String, String> kafkaTemplate = org.mockito.Mockito.mock(KafkaTemplate.class);
        when(kafkaTemplate.send(org.mockito.ArgumentMatchers.<ProducerRecord<String, String>>any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        return kafkaTemplate;
    }

    private KafkaEventConsumer consumer() {
        return new KafkaEventConsumer(
                objectMapper,
                new InventoryEventHandler(productoRepository, notificationPublisher),
                new NotificationRequestHandler(objectMapper, notificationRequestRepository));
    }
}

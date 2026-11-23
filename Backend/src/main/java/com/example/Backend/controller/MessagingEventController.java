package com.example.Backend.controller;

import com.example.Backend.messaging.InventoryUpdatedEvent;
import com.example.Backend.messaging.KafkaEventPublisher;
import com.example.Backend.messaging.NotificationRequestedEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/admin/events")
public class MessagingEventController {

    private final KafkaEventPublisher eventPublisher;

    public MessagingEventController(KafkaEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/inventory")
    public ResponseEntity<Void> actualizarInventario(@Valid @RequestBody InventoryUpdatedRequest request) {
        eventPublisher.publicarActualizacionInventario(
                new InventoryUpdatedEvent(request.productoId(), request.stock()));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/notifications")
    public ResponseEntity<Void> solicitarNotificacion(
            @Valid @RequestBody NotificationRequestedEvent event) {
        eventPublisher.publicarSolicitudNotificacion(event);
        return ResponseEntity.accepted().build();
    }

    public record InventoryUpdatedRequest(
            @NotNull @Positive Long productoId,
            @NotNull @PositiveOrZero Integer stock) {
    }
}

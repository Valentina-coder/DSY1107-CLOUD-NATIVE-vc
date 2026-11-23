package com.example.Backend.messaging;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationRequestedEvent(
        @NotBlank @Size(max = 100) String tipo,
        Long productoId,
        @Size(max = 3500) String mensaje) {
}

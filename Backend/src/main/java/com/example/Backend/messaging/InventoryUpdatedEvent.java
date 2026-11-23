package com.example.Backend.messaging;

public record InventoryUpdatedEvent(Long productoId, Integer stock) {
}

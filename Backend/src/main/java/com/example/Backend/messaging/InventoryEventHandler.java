package com.example.Backend.messaging;

import com.example.Backend.model.Producto;
import com.example.Backend.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(InventoryEventHandler.class);

    private final ProductoRepository productoRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    public InventoryEventHandler(
            ProductoRepository productoRepository,
            KafkaEventPublisher kafkaEventPublisher) {
        this.productoRepository = productoRepository;
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    @Transactional
    public void actualizarStock(InventoryUpdatedEvent event) {
        if (event == null || event.productoId() == null || event.productoId() <= 0
                || event.stock() == null || event.stock() < 0) {
            throw new IllegalArgumentException(
                    "El evento de inventario requiere productoId y stock no negativos");
        }

        Producto producto = productoRepository.findById(event.productoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el producto " + event.productoId()));
        int stockAnterior = producto.getStock();
        int stockMinimo = producto.getStockMinimo();
        boolean entraEnStockCritico = stockAnterior > stockMinimo && event.stock() <= stockMinimo;

        producto.setStock(event.stock());
        producto.setStockMinimo(stockMinimo);
        productoRepository.save(producto);
        logger.info("Stock actualizado: productoId={}, stock={}", event.productoId(), event.stock());

        if (entraEnStockCritico) {
            String mensaje = String.format(
                    "El producto %s llegó a stock crítico: stock actual %d, mínimo %d",
                    producto.getNombre(),
                    event.stock(),
                    stockMinimo);
            kafkaEventPublisher.publicarSolicitudNotificacion(
                    new NotificationRequestedEvent("stock-critico", producto.getId(), mensaje));
            logger.warn(
                    "Alerta de stock crítico publicada: productoId={}, stock={}, stockMinimo={}",
                    producto.getId(),
                    event.stock(),
                    stockMinimo);
        }
    }
}

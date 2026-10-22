package com.example.Backend.messaging;

public final class RabbitTopology {

    public static final String PRODUCT_QUEUE = "stock360.productos";
    public static final String PRODUCT_EXCHANGE = "stock360.productos.exchange";
    public static final String PRODUCT_ROUTING_KEY = "producto.creado";
    public static final String DEAD_LETTER_QUEUE = "stock360.productos.dlq";
    public static final String DEAD_LETTER_EXCHANGE = "stock360.productos.dlx";
    public static final String DEAD_LETTER_ROUTING_KEY = "producto.fallido";

    public static final String INVENTORY_QUEUE = "stock360.inventario";
    public static final String INVENTORY_ROUTING_KEY = "inventario.actualizado";
    public static final String INVENTORY_DEAD_LETTER_QUEUE = "stock360.inventario.dlq";
    public static final String INVENTORY_DEAD_LETTER_EXCHANGE = "stock360.inventario.dlx";
    public static final String INVENTORY_DEAD_LETTER_ROUTING_KEY = "inventario.fallido";

    public static final String NOTIFICATION_QUEUE = "stock360.notificaciones";
    public static final String NOTIFICATION_ROUTING_KEY = "notificacion.solicitada";
    public static final String NOTIFICATION_DEAD_LETTER_QUEUE = "stock360.notificaciones.dlq";
    public static final String NOTIFICATION_DEAD_LETTER_EXCHANGE = "stock360.notificaciones.dlx";
    public static final String NOTIFICATION_DEAD_LETTER_ROUTING_KEY = "notificacion.fallida";

    private RabbitTopology() {
    }
}

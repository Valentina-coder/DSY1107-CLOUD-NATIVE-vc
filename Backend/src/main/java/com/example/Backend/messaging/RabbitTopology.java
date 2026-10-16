package com.example.Backend.messaging;

public final class RabbitTopology {

    public static final String PRODUCT_QUEUE = "stock360.productos";
    public static final String PRODUCT_EXCHANGE = "stock360.productos.exchange";
    public static final String PRODUCT_ROUTING_KEY = "producto.creado";
    public static final String DEAD_LETTER_QUEUE = "stock360.productos.dlq";
    public static final String DEAD_LETTER_EXCHANGE = "stock360.productos.dlx";
    public static final String DEAD_LETTER_ROUTING_KEY = "producto.fallido";

    private RabbitTopology() {
    }
}

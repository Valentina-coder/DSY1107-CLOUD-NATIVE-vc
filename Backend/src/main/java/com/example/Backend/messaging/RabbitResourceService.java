package com.example.Backend.messaging;

import java.util.HashMap;
import java.util.Map;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;

@Service
public class RabbitResourceService {

    private final AmqpAdmin amqpAdmin;

    public RabbitResourceService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    public String crearCola(QueueRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La configuración de la cola es obligatoria");
        }
        validarNombreGestionable(request.name());
        boolean hasDeadLetterExchange = request.deadLetterExchange() != null
                && !request.deadLetterExchange().isBlank();
        boolean hasDeadLetterRoutingKey = request.deadLetterRoutingKey() != null
                && !request.deadLetterRoutingKey().isBlank();
        if (hasDeadLetterExchange != hasDeadLetterRoutingKey) {
            throw new IllegalArgumentException(
                    "deadLetterExchange y deadLetterRoutingKey deben enviarse juntos");
        }

        Map<String, Object> arguments = new HashMap<>();
        if (hasDeadLetterExchange) {
            arguments.put("x-dead-letter-exchange", request.deadLetterExchange());
            arguments.put("x-dead-letter-routing-key", request.deadLetterRoutingKey());
        }

        Queue queue = new Queue(
                request.name(),
                request.durable(),
                request.exclusive(),
                request.autoDelete(),
                arguments);
        return amqpAdmin.declareQueue(queue);
    }

    public void eliminarCola(String name) {
        validarNombreGestionable(name);
        amqpAdmin.deleteQueue(name);
    }

    public String crearExchange(ExchangeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La configuración del exchange es obligatoria");
        }
        validarNombreGestionable(request.name());
        if (request.type() == null) {
            throw new IllegalArgumentException("El tipo de exchange es obligatorio");
        }
        Exchange exchange = switch (request.type()) {
            case DIRECT -> new DirectExchange(request.name(), request.durable(), request.autoDelete());
            case FANOUT -> new FanoutExchange(request.name(), request.durable(), request.autoDelete());
            case TOPIC -> new TopicExchange(request.name(), request.durable(), request.autoDelete());
            case HEADERS -> new HeadersExchange(request.name(), request.durable(), request.autoDelete());
        };
        amqpAdmin.declareExchange(exchange);
        return exchange.getName();
    }

    public void eliminarExchange(String name) {
        validarNombreGestionable(name);
        amqpAdmin.deleteExchange(name);
    }

    public void crearBinding(BindingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La configuración del binding es obligatoria");
        }
        validarNombreGestionable(request.queue());
        validarNombreGestionable(request.exchange());
        amqpAdmin.declareBinding(new Binding(
                request.queue(),
                Binding.DestinationType.QUEUE,
                request.exchange(),
                request.routingKey() == null ? "" : request.routingKey(),
                null));
    }

    public void eliminarBinding(BindingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La configuración del binding es obligatoria");
        }
        validarNombreGestionable(request.queue());
        validarNombreGestionable(request.exchange());
        amqpAdmin.removeBinding(new Binding(
                request.queue(),
                Binding.DestinationType.QUEUE,
                request.exchange(),
                request.routingKey() == null ? "" : request.routingKey(),
                null));
    }

    private void validarNombreGestionable(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del recurso es obligatorio");
        }
        if (name.startsWith("amq.") || esRecursoDelSistema(name)) {
            throw new IllegalArgumentException("No se permite modificar un recurso reservado");
        }
    }

    private boolean esRecursoDelSistema(String name) {
        return name.equals(RabbitTopology.PRODUCT_QUEUE)
                || name.equals(RabbitTopology.PRODUCT_EXCHANGE)
                || name.equals(RabbitTopology.DEAD_LETTER_QUEUE)
                || name.equals(RabbitTopology.DEAD_LETTER_EXCHANGE);
    }

    public enum ExchangeType {
        DIRECT,
        FANOUT,
        TOPIC,
        HEADERS
    }

    public record QueueRequest(
            @NotBlank String name,
            boolean durable,
            boolean exclusive,
            boolean autoDelete,
            String deadLetterExchange,
            String deadLetterRoutingKey) {
    }

    public record ExchangeRequest(
            @NotBlank String name,
            @NotNull ExchangeType type,
            boolean durable,
            boolean autoDelete) {
    }

    public record BindingRequest(
            @NotBlank String queue,
            @NotBlank String exchange,
            String routingKey) {
    }
}

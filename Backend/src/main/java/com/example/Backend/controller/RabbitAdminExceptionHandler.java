package com.example.Backend.controller;

import java.util.Map;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RabbitAdminController.class)
public class RabbitAdminExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RabbitAdminExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarSolicitudInvalida(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Map<String, String>> manejarErrorRabbit(AmqpException exception) {
        logger.error("Falló una operación administrativa de RabbitMQ", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "No se pudo completar la operación en RabbitMQ"));
    }
}

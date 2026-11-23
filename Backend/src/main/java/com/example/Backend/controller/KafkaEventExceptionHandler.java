package com.example.Backend.controller;

import com.example.Backend.messaging.KafkaPublishException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = MessagingEventController.class)
public class KafkaEventExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventExceptionHandler.class);

    @ExceptionHandler(KafkaPublishException.class)
    public ResponseEntity<Map<String, String>> errorPublicando(KafkaPublishException exception) {
        logger.error("No se pudo publicar el evento en Kafka", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Kafka no pudo confirmar la publicación del evento"));
    }
}

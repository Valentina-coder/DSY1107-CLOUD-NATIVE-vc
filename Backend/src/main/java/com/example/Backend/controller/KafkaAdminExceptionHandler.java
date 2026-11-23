package com.example.Backend.controller;

import com.example.Backend.messaging.KafkaAdminOperationException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = KafkaAdminController.class)
public class KafkaAdminExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(KafkaAdminExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> solicitudInvalida(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(KafkaAdminOperationException.class)
    public ResponseEntity<Map<String, String>> errorKafka(KafkaAdminOperationException exception) {
        logger.error("Falló una operación administrativa de Kafka", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", exception.getMessage()));
    }
}

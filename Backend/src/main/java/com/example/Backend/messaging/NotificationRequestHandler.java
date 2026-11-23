package com.example.Backend.messaging;

import com.example.Backend.model.NotificationRequest;
import com.example.Backend.repository.NotificationRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationRequestHandler.class);

    private final ObjectMapper objectMapper;
    private final NotificationRequestRepository notificationRequestRepository;

    public NotificationRequestHandler(
            ObjectMapper objectMapper,
            NotificationRequestRepository notificationRequestRepository) {
        this.objectMapper = objectMapper;
        this.notificationRequestRepository = notificationRequestRepository;
    }

    public void registrar(String payload) {
        final JsonNode event;
        try {
            event = objectMapper.readTree(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Solicitud de notificación inválida", exception);
        }

        JsonNode typeNode = event == null ? null : event.get("tipo");
        String type = typeNode != null && typeNode.isTextual() ? typeNode.asText().trim() : "";
        if (event == null || !event.isObject() || type.isBlank() || type.length() > 100
                || payload == null || payload.length() > 4000) {
            throw new IllegalArgumentException(
                    "La solicitud requiere un tipo y un payload de hasta 4000 caracteres");
        }

        NotificationRequest saved = notificationRequestRepository.save(
                new NotificationRequest(type, payload, Instant.now()));
        logger.info("Solicitud de notificación registrada: id={}, tipo={}", saved.getId(), type);
    }
}

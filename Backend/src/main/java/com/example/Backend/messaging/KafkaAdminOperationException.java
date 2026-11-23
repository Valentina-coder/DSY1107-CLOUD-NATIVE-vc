package com.example.Backend.messaging;

public class KafkaAdminOperationException extends RuntimeException {

    public KafkaAdminOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

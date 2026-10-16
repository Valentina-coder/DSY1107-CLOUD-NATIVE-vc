package com.example.Backend.controller;

import com.example.Backend.messaging.RabbitResourceService;
import com.example.Backend.messaging.RabbitResourceService.BindingRequest;
import com.example.Backend.messaging.RabbitResourceService.ExchangeRequest;
import com.example.Backend.messaging.RabbitResourceService.QueueRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rabbitmq")
public class RabbitAdminController {

    private final RabbitResourceService resourceService;

    public RabbitAdminController(RabbitResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping("/queues")
    public ResponseEntity<String> crearCola(@Valid @RequestBody QueueRequest request) {
        String queueName = resourceService.crearCola(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(queueName);
    }

    @DeleteMapping("/queues")
    public ResponseEntity<Void> eliminarCola(@RequestParam String name) {
        resourceService.eliminarCola(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<String> crearExchange(@Valid @RequestBody ExchangeRequest request) {
        String exchangeName = resourceService.crearExchange(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(exchangeName);
    }

    @DeleteMapping("/exchanges")
    public ResponseEntity<Void> eliminarExchange(@RequestParam String name) {
        resourceService.eliminarExchange(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<Void> crearBinding(@Valid @RequestBody BindingRequest request) {
        resourceService.crearBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(
            @RequestParam String queue,
            @RequestParam String exchange,
            @RequestParam(defaultValue = "") String routingKey) {
        resourceService.eliminarBinding(new BindingRequest(queue, exchange, routingKey));
        return ResponseEntity.noContent().build();
    }
}

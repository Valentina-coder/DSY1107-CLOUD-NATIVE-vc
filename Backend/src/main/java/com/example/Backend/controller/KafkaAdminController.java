package com.example.Backend.controller;

import com.example.Backend.messaging.KafkaTopicService;
import com.example.Backend.messaging.KafkaTopicService.CreateTopicRequest;
import com.example.Backend.messaging.KafkaTopicService.TopicInfo;
import com.example.Backend.messaging.KafkaTopicService.UpdateTopicRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/kafka/topics")
public class KafkaAdminController {

    private final KafkaTopicService topicService;

    public KafkaAdminController(KafkaTopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public List<TopicInfo> listarTopics() {
        return topicService.listarTopics();
    }

    @PostMapping
    public ResponseEntity<String> crearTopic(@Valid @RequestBody CreateTopicRequest request) {
        topicService.crearTopic(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(request.name());
    }

    @PatchMapping("/{name}")
    public ResponseEntity<Void> modificarTopic(
            @PathVariable String name,
            @Valid @RequestBody UpdateTopicRequest request) {
        topicService.modificarTopic(name, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{name}/configs")
    public Map<String, String> obtenerConfiguraciones(@PathVariable String name) {
        return topicService.obtenerConfiguraciones(name);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> eliminarTopic(@PathVariable String name) {
        topicService.eliminarTopic(name);
        return ResponseEntity.noContent().build();
    }
}

package com.example.Backend.messaging;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.stereotype.Service;

@Service
public class KafkaTopicService {

    private static final long OPERATION_TIMEOUT_SECONDS = 10;
    private static final String TOPIC_NAME_PATTERN = "^[a-zA-Z0-9._-]+$";

    private final AdminClient adminClient;

    public KafkaTopicService(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    public List<TopicInfo> listarTopics() {
        Set<String> names = esperar(adminClient.listTopics().names(), "listar tópicos").stream()
                .filter(name -> !name.startsWith("__"))
                .sorted()
                .collect(Collectors.toSet());
        if (names.isEmpty()) {
            return List.of();
        }

        Map<String, TopicDescription> descriptions = esperar(
                adminClient.describeTopics(names).allTopicNames(), "describir tópicos");
        return descriptions.values().stream()
                .map(description -> new TopicInfo(
                        description.name(),
                        description.partitions().size(),
                        description.partitions().get(0).replicas().size()))
                .sorted((first, second) -> first.name().compareTo(second.name()))
                .toList();
    }

    public void crearTopic(CreateTopicRequest request) {
        validarNombre(request.name());
        NewTopic topic = new NewTopic(
                request.name(),
                request.partitions(),
                request.replicationFactor().shortValue());
        validarConfiguraciones(request.configs());
        if (request.configs() != null && !request.configs().isEmpty()) {
            topic.configs(request.configs());
        }
        esperar(adminClient.createTopics(List.of(topic)).all(), "crear tópico");
    }

    public void modificarTopic(String name, UpdateTopicRequest request) {
        validarNombre(name);
        Map<String, String> configs = request.configs() == null ? Map.of() : request.configs();
        if (request.partitions() == null && configs.isEmpty()) {
            throw new IllegalArgumentException("Indica nuevas particiones o configuraciones");
        }
        validarConfiguraciones(configs);

        if (request.partitions() != null) {
            TopicDescription description = esperar(
                    adminClient.describeTopics(List.of(name)).allTopicNames(),
                    "consultar tópico").get(name);
            int currentPartitions = description.partitions().size();
            if (request.partitions() < currentPartitions) {
                throw new IllegalArgumentException("Kafka no permite reducir la cantidad de particiones");
            }
            if (request.partitions() > currentPartitions) {
                esperar(adminClient.createPartitions(Map.of(
                        name, NewPartitions.increaseTo(request.partitions()))).all(),
                        "aumentar particiones");
            }
        }

        if (!configs.isEmpty()) {
            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
            List<AlterConfigOp> operations = configs.entrySet().stream()
                    .map(entry -> new AlterConfigOp(
                            new ConfigEntry(entry.getKey(), entry.getValue()),
                            AlterConfigOp.OpType.SET))
                    .toList();
            esperar(adminClient.incrementalAlterConfigs(Map.of(resource, operations)).all(),
                    "modificar configuraciones del tópico");
        }
    }

    public Map<String, String> obtenerConfiguraciones(String name) {
        validarNombre(name);
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        Map<ConfigResource, Config> configs = esperar(
                adminClient.describeConfigs(List.of(resource)).all(), "consultar configuraciones");
        Map<String, String> result = new LinkedHashMap<>();
        configs.get(resource).entries().stream()
                .sorted((first, second) -> first.name().compareTo(second.name()))
                .forEach(entry -> result.put(
                        entry.name(),
                        entry.isSensitive() ? "[REDACTED]" : String.valueOf(entry.value())));
        return result;
    }

    public void eliminarTopic(String name) {
        validarNombre(name);
        esperar(adminClient.deleteTopics(List.of(name)).all(), "eliminar tópico");
    }

    private void validarNombre(String name) {
        if (name == null || name.isBlank() || name.length() > 249
                || !name.matches(TOPIC_NAME_PATTERN) || name.startsWith("__")) {
            throw new IllegalArgumentException("El nombre del tópico no es válido o está reservado");
        }
    }

    private void validarConfiguraciones(Map<String, String> configs) {
        if (configs == null) {
            return;
        }
        configs.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null) {
                throw new IllegalArgumentException("Las configuraciones requieren clave y valor");
            }
        });
    }

    private <T> T esperar(KafkaFuture<T> future, String operacion) {
        try {
            return future.get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminOperationException("Se interrumpió la operación: " + operacion, exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new KafkaAdminOperationException("No se pudo completar la operación: " + operacion, exception);
        }
    }

    public record TopicInfo(String name, int partitions, int replicationFactor) {
    }

    public record CreateTopicRequest(
            @NotBlank @Size(max = 249) @Pattern(regexp = TOPIC_NAME_PATTERN) String name,
            @NotNull @Positive @Max(100) Integer partitions,
            @NotNull @Positive @Max(3) Short replicationFactor,
            @Size(max = 50) Map<String, String> configs) {
    }

    public record UpdateTopicRequest(
            @Positive @Max(100) Integer partitions,
            @Size(max = 50) Map<String, String> configs) {
    }
}

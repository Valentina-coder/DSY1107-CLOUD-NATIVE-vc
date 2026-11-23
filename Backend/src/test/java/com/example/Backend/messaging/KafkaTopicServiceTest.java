package com.example.Backend.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.Test;

class KafkaTopicServiceTest {

    private final AdminClient adminClient = org.mockito.Mockito.mock(AdminClient.class);
    private final KafkaTopicService topicService = new KafkaTopicService(adminClient);

    @Test
    void preventsChangesToKafkaInternalTopics() {
        assertThatThrownBy(() -> topicService.eliminarTopic("__consumer_offsets"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reservado");
        verifyNoInteractions(adminClient);
    }

    @Test
    void requiresAnActualTopicChange() {
        assertThatThrownBy(() -> topicService.modificarTopic(
                "stock360.test", new KafkaTopicService.UpdateTopicRequest(null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Indica");
        verifyNoInteractions(adminClient);
    }
}

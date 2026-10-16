package com.example.Backend.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.Backend.messaging.RabbitResourceService.BindingRequest;
import com.example.Backend.messaging.RabbitResourceService.ExchangeRequest;
import com.example.Backend.messaging.RabbitResourceService.ExchangeType;
import com.example.Backend.messaging.RabbitResourceService.QueueRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;

@ExtendWith(MockitoExtension.class)
class RabbitResourceServiceTest {

    @Mock
    private AmqpAdmin amqpAdmin;

    @InjectMocks
    private RabbitResourceService resourceService;

    @Test
    void declaresDurableQueueWithDeadLetterRouting() {
        QueueRequest request = new QueueRequest(
                "stock360.test.queue", true, false, false, "stock360.test.dlx", "failed");
        when(amqpAdmin.declareQueue(org.mockito.ArgumentMatchers.any(Queue.class)))
                .thenReturn(request.name());

        String name = resourceService.crearCola(request);

        ArgumentCaptor<Queue> queueCaptor = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(queueCaptor.capture());
        assertThat(name).isEqualTo(request.name());
        assertThat(queueCaptor.getValue().getArguments())
                .containsEntry("x-dead-letter-exchange", "stock360.test.dlx")
                .containsEntry("x-dead-letter-routing-key", "failed");
    }

    @Test
    void rejectsQueueWithIncompleteDeadLetterConfiguration() {
        QueueRequest request = new QueueRequest(
                "stock360.test.queue", true, false, false, "stock360.test.dlx", null);

        assertThatThrownBy(() -> resourceService.crearCola(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deben enviarse juntos");
        verifyNoInteractions(amqpAdmin);
    }

    @Test
    void doesNotAllowDeletingTheApplicationDeadLetterQueue() {
        assertThatThrownBy(() -> resourceService.eliminarCola(RabbitTopology.DEAD_LETTER_QUEUE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recurso reservado");
        verifyNoInteractions(amqpAdmin);
    }

    @Test
    void declaresExchangeWithRequestedTypeAndDurability() {
        ExchangeRequest request = new ExchangeRequest("stock360.test.topic", ExchangeType.TOPIC, true, false);

        String name = resourceService.crearExchange(request);

        ArgumentCaptor<Exchange> exchangeCaptor = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin).declareExchange(exchangeCaptor.capture());
        assertThat(name).isEqualTo(request.name());
        assertThat(exchangeCaptor.getValue().getType()).isEqualTo("topic");
        assertThat(exchangeCaptor.getValue().isDurable()).isTrue();
    }

    @Test
    void createsAndRemovesQueueBinding() {
        BindingRequest request = new BindingRequest("stock360.test.queue", "stock360.test.exchange", "events.created");

        resourceService.crearBinding(request);
        resourceService.eliminarBinding(request);

        ArgumentCaptor<Binding> bindingCaptor = ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).declareBinding(bindingCaptor.capture());
        verify(amqpAdmin).removeBinding(bindingCaptor.capture());
        assertThat(bindingCaptor.getAllValues())
                .allSatisfy(binding -> {
                    assertThat(binding.getDestination()).isEqualTo(request.queue());
                    assertThat(binding.getExchange()).isEqualTo(request.exchange());
                    assertThat(binding.getRoutingKey()).isEqualTo(request.routingKey());
                });
    }
}

package com.arcticsurge.cosmolab.infrastructure.messaging;

import com.arcticsurge.cosmolab.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClinicalEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishClinicalEvent(String routingKey, UUID aggregateId, String payload) {
        ClinicalEvent event = ClinicalEvent.of(routingKey, aggregateId, payload);
        rabbitTemplate.convertAndSend(RabbitMQConfig.CLINICAL_EVENTS_EXCHANGE, routingKey, event);
        log.debug("Published clinical event: {} for {}", routingKey, aggregateId);
    }

    public void publishAuditEvent(UUID aggregateId, String payload) {
        ClinicalEvent event = ClinicalEvent.of("audit", aggregateId, payload);
        rabbitTemplate.convertAndSend(RabbitMQConfig.AUDIT_EXCHANGE, RabbitMQConfig.AUDIT_ROUTING_KEY, event);
        log.debug("Published audit event for {}", aggregateId);
    }
}

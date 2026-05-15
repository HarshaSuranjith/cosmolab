package com.arcticsurge.cosmolab.infrastructure.messaging;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Profile("!test")
@Slf4j
public class AuditEventConsumer {

    @RabbitListener(queues = "audit-queue", ackMode = "MANUAL")
    public void consume(ClinicalEvent event, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("Audit event received: {} aggregateId={}", event.eventType(), event.aggregateId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process audit event: {}", event.eventId(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}

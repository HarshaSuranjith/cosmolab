package com.arcticsurge.cosmolab.infrastructure.messaging;

import com.arcticsurge.cosmolab.infrastructure.persistence.AuditEventRecord;
import com.arcticsurge.cosmolab.infrastructure.persistence.AuditEventRecordRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditEventRecordRepository auditEventRecordRepository;

    @RabbitListener(queues = "audit-queue", ackMode = "MANUAL")
    public void consume(ClinicalEvent event, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            if (!auditEventRecordRepository.existsByEventId(event.eventId())) {
                AuditEventRecord record = new AuditEventRecord();
                record.setEventId(event.eventId());
                record.setEventType(event.eventType());
                record.setAggregateId(event.aggregateId());
                record.setPayload(event.payload());
                record.setOccurredAt(event.occurredAt());
                auditEventRecordRepository.save(record);
                log.info("Audit event persisted: {} aggregateId={}", event.eventType(), event.aggregateId());
            } else {
                log.debug("Duplicate audit event ignored: {}", event.eventId());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process audit event: {}", event.eventId(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}

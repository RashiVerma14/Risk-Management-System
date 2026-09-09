package com.riskmanager.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(String topic, String key, Object payload) {
        try {
            DomainEvent<Object> event = DomainEvent.of(topic, key, payload);
            kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.warn("Asynchronous send failed to topic {}: {}", topic, ex.getMessage());
                } else {
                    log.debug("Sent event to topic {} with offset {}", topic, result.getRecordMetadata().offset());
                }
            });
        } catch (Exception ex) {
            log.warn("Kafka unavailable or failed to publish event to topic {}: {}", topic, ex.getMessage());
        }
    }
}

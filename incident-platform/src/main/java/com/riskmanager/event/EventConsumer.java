package com.riskmanager.event;

import com.riskmanager.notification.NotificationDispatcher;
import com.riskmanager.notification.NotificationPayload;
import com.riskmanager.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class EventConsumer {

    private final NotificationDispatcher notificationDispatcher;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = KafkaTopicConfig.TOPIC_NOTIFICATION, groupId = "riskmanager-notification-group")
    public void consumeNotificationEvent(
            @Payload DomainEvent<NotificationPayload> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.info("Received event from topic {}: {}", topic, event.getEventType());
        if (event.getPayload() != null) {
            notificationDispatcher.dispatch(event.getPayload());
        }
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_INCIDENT_CREATED, groupId = "riskmanager-incident-group")
    public void consumeIncidentCreatedEvent(
            @Payload DomainEvent<Object> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.info("Asynchronously processed incident created event: {}", event.getAggregateId());
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_ALERT_CREATED, groupId = "riskmanager-alert-group")
    public void consumeAlertCreatedEvent(
            @Payload DomainEvent<Object> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.info("Asynchronously processed alert created event: {}", event.getAggregateId());
    }

    @DltHandler
    public void handleDeadLetterTopic(
            Object payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {

        log.error("Dead-letter topic handler invoked for topic {}: {}", topic, exceptionMessage);
    }
}

package com.riskmanager.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    public static final String TOPIC_INCIDENT_CREATED = "riskmanager.incident.created";
    public static final String TOPIC_INCIDENT_UPDATED = "riskmanager.incident.updated";
    public static final String TOPIC_ALERT_CREATED = "riskmanager.alert.created";
    public static final String TOPIC_MONITORING_EVENTS = "riskmanager.monitoring.events";
    public static final String TOPIC_AI_ANALYSIS = "riskmanager.ai.analysis";
    public static final String TOPIC_NOTIFICATION = "riskmanager.notification";
    public static final String TOPIC_DEPLOYMENT_EVENTS = "riskmanager.deployment.events";

    @Bean
    public NewTopic incidentCreatedTopic() {
        return TopicBuilder.name(TOPIC_INCIDENT_CREATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic incidentUpdatedTopic() {
        return TopicBuilder.name(TOPIC_INCIDENT_UPDATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic alertCreatedTopic() {
        return TopicBuilder.name(TOPIC_ALERT_CREATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic monitoringEventsTopic() {
        return TopicBuilder.name(TOPIC_MONITORING_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic aiAnalysisTopic() {
        return TopicBuilder.name(TOPIC_AI_ANALYSIS).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name(TOPIC_NOTIFICATION).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic deploymentEventsTopic() {
        return TopicBuilder.name(TOPIC_DEPLOYMENT_EVENTS).partitions(2).replicas(1).build();
    }
}

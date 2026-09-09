package com.riskmanager.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent<T> {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    private String eventType;
    private String aggregateId;
    @Builder.Default
    private Instant timestamp = Instant.now();
    private T payload;

    public static <T> DomainEvent<T> of(String eventType, String aggregateId, T payload) {
        return DomainEvent.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .aggregateId(aggregateId)
                .timestamp(Instant.now())
                .payload(payload)
                .build();
    }
}

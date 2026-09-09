package com.riskmanager.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DomainEventSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void testDomainEventJsonSerialization() throws Exception {
        Map<String, Object> payload = Map.of("incidentId", "inc-101", "service", "billing");
        DomainEvent<Map<String, Object>> event = DomainEvent.of("INCIDENT_CREATED", "inc-101", payload);

        String json = objectMapper.writeValueAsString(event);
        assertNotNull(json);
        assertTrue(json.contains("INCIDENT_CREATED"));
        assertTrue(json.contains("inc-101"));

        DomainEvent deserialized = objectMapper.readValue(json, DomainEvent.class);
        assertEquals("INCIDENT_CREATED", deserialized.getEventType());
        assertEquals("inc-101", deserialized.getAggregateId());
        assertNotNull(deserialized.getTimestamp());
    }
}

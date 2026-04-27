package com.smartcommerce.common.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseEvent<T> {
    private UUID eventId;
    private String eventType;
    private String aggregateId;
    private String aggregateType;
    private Integer version;
    private Instant occurredAt;
    private String correlationId;
    private String causationId;
    private T payload;

    public static <T> BaseEvent<T> create(String eventType, String aggregateId, String aggregateType, T payload) {
        return BaseEvent.<T>builder()
            .eventId(UUID.randomUUID())
            .eventType(eventType)
            .aggregateId(aggregateId)
            .aggregateType(aggregateType)
            .version(1)
            .occurredAt(Instant.now())
            .payload(payload)
            .build();
    }
}

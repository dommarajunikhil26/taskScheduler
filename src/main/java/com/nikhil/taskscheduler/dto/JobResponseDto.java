package com.nikhil.taskscheduler.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.nikhil.taskscheduler.dao.Status;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class JobResponseDto {
    private UUID uuid;
    private String name;
    private Status status;
    private Instant scheduledAt;
    private Instant createdAt;
    private int maxRetries;
    private JsonNode payload;
}

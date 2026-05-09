package com.nikhil.taskscheduler.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class JobRequestDto {
    @Size(max = 255)
    private String name;
    @NotNull(message = "scheduledAt should not be null")
    private Instant scheduledAt;
    @Min(1) @Max(10)
    private int maxRetries = 3;
    @NotNull(message = "payload is required")
    private JsonNode payload;
}

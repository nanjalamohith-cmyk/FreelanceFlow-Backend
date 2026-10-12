package com.freelanceflow.dto;

import com.freelanceflow.model.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {
    private String id;
    private String clientId;
    private String clientName;
    private String title;
    private String description;
    private BigDecimal budget;
    private List<String> skills;
    private Instant deadline;
    private JobStatus status;
    private long bidCount;
    private Instant createdAt;
}

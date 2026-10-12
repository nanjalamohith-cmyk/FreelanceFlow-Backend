package com.freelanceflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "jobs")
public class Job {
    @Id
    private String id;

    private String clientId;

    private String title;

    private String description;

    private BigDecimal budget;

    @Builder.Default
    private List<String> skills = new ArrayList<>();

    private Instant deadline;

    @Indexed
    @Builder.Default
    private JobStatus status = JobStatus.OPEN;

    @Builder.Default
    private Instant createdAt = Instant.now();
}

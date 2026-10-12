package com.freelanceflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "disputes")
public class Dispute {
    @Id
    private String id;

    private String contractId;

    private String milestoneId;

    private String raisedBy;

    private String reason;

    @Builder.Default
    private DisputeStatus status = DisputeStatus.OPEN;

    private DisputeResolution resolution;

    private Double splitPercentToFreelancer;

    private String adminNote;

    @Builder.Default
    private Instant createdAt = Instant.now();
}

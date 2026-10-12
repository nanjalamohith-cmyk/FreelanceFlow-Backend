package com.freelanceflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "milestones")
public class Milestone {
    @Id
    private String id;

    private String contractId;

    private String title;

    private String description;

    private BigDecimal amount;

    private Instant dueDate;

    @Builder.Default
    private MilestoneStatus status = MilestoneStatus.PENDING_FUNDING;

    @Builder.Default
    private List<Deliverable> deliverables = new ArrayList<>();

    private String revisionNote;

    @Builder.Default
    private Integer order = 1;
}

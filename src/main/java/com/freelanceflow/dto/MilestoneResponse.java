package com.freelanceflow.dto;

import com.freelanceflow.model.Deliverable;
import com.freelanceflow.model.Milestone;
import com.freelanceflow.model.MilestoneStatus;
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
public class MilestoneResponse {
    private String id;
    private String contractId;
    private String title;
    private String description;
    private BigDecimal amount;
    private Instant dueDate;
    private MilestoneStatus status;
    private List<Deliverable> deliverables;
    private String revisionNote;
    private Integer order;

    public static MilestoneResponse fromMilestone(Milestone m) {
        if (m == null) return null;
        return MilestoneResponse.builder()
                .id(m.getId())
                .contractId(m.getContractId())
                .title(m.getTitle())
                .description(m.getDescription())
                .amount(m.getAmount())
                .dueDate(m.getDueDate())
                .status(m.getStatus())
                .deliverables(m.getDeliverables())
                .revisionNote(m.getRevisionNote())
                .order(m.getOrder())
                .build();
    }
}

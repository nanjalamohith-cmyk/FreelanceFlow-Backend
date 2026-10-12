package com.freelanceflow.dto;

import com.freelanceflow.model.Dispute;
import com.freelanceflow.model.DisputeResolution;
import com.freelanceflow.model.DisputeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeResponse {
    private String id;
    private String contractId;
    private String milestoneId;
    private String milestoneTitle;
    private BigDecimal milestoneAmount;
    private String raisedBy;
    private String raisedByName;
    private String reason;
    private DisputeStatus status;
    private DisputeResolution resolution;
    private Double splitPercentToFreelancer;
    private String adminNote;
    private Instant createdAt;

    public static DisputeResponse fromDispute(Dispute d) {
        if (d == null) return null;
        return DisputeResponse.builder()
                .id(d.getId())
                .contractId(d.getContractId())
                .milestoneId(d.getMilestoneId())
                .raisedBy(d.getRaisedBy())
                .reason(d.getReason())
                .status(d.getStatus())
                .resolution(d.getResolution())
                .splitPercentToFreelancer(d.getSplitPercentToFreelancer())
                .adminNote(d.getAdminNote())
                .createdAt(d.getCreatedAt())
                .build();
    }
}

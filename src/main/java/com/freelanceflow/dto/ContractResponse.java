package com.freelanceflow.dto;

import com.freelanceflow.model.ContractStatus;
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
public class ContractResponse {
    private String id;
    private String jobId;
    private String jobTitle;
    private String clientId;
    private String clientName;
    private String freelancerId;
    private String freelancerName;
    private BigDecimal totalAmount;
    private ContractStatus status;
    private List<MilestoneResponse> milestones;
    private Instant createdAt;
}

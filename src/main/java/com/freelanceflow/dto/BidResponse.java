package com.freelanceflow.dto;

import com.freelanceflow.model.BidStatus;
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
public class BidResponse {
    private String id;
    private String jobId;
    private String jobTitle;
    private String freelancerId;
    private String freelancerName;
    private Double freelancerRatingAvg;
    private Integer freelancerRatingCount;
    private BigDecimal amount;
    private Integer deliveryDays;
    private String proposal;
    private BidStatus status;
    private Instant createdAt;
}

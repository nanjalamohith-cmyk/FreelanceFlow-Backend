package com.freelanceflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStatsResponse {
    private long totalUsers;
    private long totalClients;
    private long totalFreelancers;
    private long totalJobs;
    private long activeContracts;
    private long completedContracts;
    private BigDecimal totalEscrowVolume;
    private long openDisputes;
}

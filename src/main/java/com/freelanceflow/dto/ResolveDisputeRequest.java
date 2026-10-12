package com.freelanceflow.dto;

import com.freelanceflow.model.DisputeResolution;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveDisputeRequest {
    @NotNull(message = "Resolution is required")
    private DisputeResolution resolution;

    private Double splitPercentToFreelancer;

    private String adminNote;
}

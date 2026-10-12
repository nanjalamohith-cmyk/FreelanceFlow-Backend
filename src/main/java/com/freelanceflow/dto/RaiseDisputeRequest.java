package com.freelanceflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaiseDisputeRequest {
    @NotBlank(message = "Reason for dispute is required")
    private String reason;
}

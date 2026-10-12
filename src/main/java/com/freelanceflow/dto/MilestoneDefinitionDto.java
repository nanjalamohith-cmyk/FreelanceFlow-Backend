package com.freelanceflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class MilestoneDefinitionDto {
    @NotBlank(message = "Milestone title is required")
    private String title;

    private String description;

    @NotNull(message = "Milestone amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least $1.00")
    private BigDecimal amount;

    private Instant dueDate;

    private Integer order;
}

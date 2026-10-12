package com.freelanceflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBidRequest {
    @NotNull(message = "Bid amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least $1.00")
    private BigDecimal amount;

    @NotNull(message = "Delivery days is required")
    @Min(value = 1, message = "Delivery days must be at least 1")
    private Integer deliveryDays;

    @NotBlank(message = "Proposal details are required")
    private String proposal;
}

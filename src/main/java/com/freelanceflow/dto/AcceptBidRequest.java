package com.freelanceflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptBidRequest {
    @NotEmpty(message = "At least one milestone must be defined")
    @Valid
    private List<MilestoneDefinitionDto> milestones;
}

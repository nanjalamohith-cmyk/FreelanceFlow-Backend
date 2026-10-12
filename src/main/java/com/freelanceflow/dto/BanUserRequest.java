package com.freelanceflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BanUserRequest {
    @NotNull(message = "Banned status is required")
    private Boolean isBanned;
}

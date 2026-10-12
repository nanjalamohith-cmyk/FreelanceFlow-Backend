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
public class SubmitDeliverableRequest {
    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "Deliverable URL is required")
    private String url;
}

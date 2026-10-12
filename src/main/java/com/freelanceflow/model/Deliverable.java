package com.freelanceflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deliverable {
    private String fileName;
    private String url;
    @Builder.Default
    private Instant uploadedAt = Instant.now();
}

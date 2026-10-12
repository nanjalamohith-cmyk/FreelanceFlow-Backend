package com.freelanceflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private String id;
    private String contractId;
    private String reviewerId;
    private String reviewerName;
    private String revieweeId;
    private String revieweeName;
    private Integer rating;
    private String comment;
    private Instant createdAt;
}

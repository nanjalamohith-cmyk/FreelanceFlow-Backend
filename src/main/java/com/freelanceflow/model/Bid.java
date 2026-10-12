package com.freelanceflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bids")
@CompoundIndex(name = "job_freelancer_idx", def = "{'jobId': 1, 'freelancerId': 1}", unique = true)
public class Bid {
    @Id
    private String id;

    private String jobId;

    private String freelancerId;

    private BigDecimal amount;

    private Integer deliveryDays;

    private String proposal;

    @Builder.Default
    private BidStatus status = BidStatus.PENDING;

    @Builder.Default
    private Instant createdAt = Instant.now();
}

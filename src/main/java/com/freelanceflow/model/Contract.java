package com.freelanceflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "contracts")
public class Contract {
    @Id
    private String id;

    private String jobId;

    private String clientId;

    private String freelancerId;

    private BigDecimal totalAmount;

    @Builder.Default
    private ContractStatus status = ContractStatus.ACTIVE;

    @Builder.Default
    private Instant createdAt = Instant.now();
}

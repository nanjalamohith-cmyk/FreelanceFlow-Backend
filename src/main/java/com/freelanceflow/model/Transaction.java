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
@Document(collection = "transactions")
public class Transaction {
    @Id
    private String id;

    private String userId;

    private String contractId;

    private String milestoneId;

    private TransactionType type;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    @Builder.Default
    private Instant createdAt = Instant.now();
}

package com.freelanceflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;

    private String name;

    @Indexed(unique = true, name = "email")
    private String email;

    private String passwordHash;

    private Role role;

    @Builder.Default
    private List<String> skills = new ArrayList<>();

    private String bio;

    @Builder.Default
    private List<String> portfolioLinks = new ArrayList<>();

    private String companyName;

    @Builder.Default
    private Double ratingAvg = 0.0;

    @Builder.Default
    private Integer ratingCount = 0;

    @Builder.Default
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @Builder.Default
    private Boolean isBanned = false;

    @Builder.Default
    private Instant createdAt = Instant.now();
}

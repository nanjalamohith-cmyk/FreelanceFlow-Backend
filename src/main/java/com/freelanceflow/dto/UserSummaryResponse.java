package com.freelanceflow.dto;

import com.freelanceflow.model.Role;
import com.freelanceflow.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
    private String id;
    private String name;
    private String email;
    private Role role;
    private List<String> skills;
    private String bio;
    private List<String> portfolioLinks;
    private String companyName;
    private Double ratingAvg;
    private Integer ratingCount;
    private BigDecimal walletBalance;
    private Boolean isBanned;
    private Instant createdAt;

    public static UserSummaryResponse fromUser(User user) {
        if (user == null) return null;
        return UserSummaryResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .skills(user.getSkills())
                .bio(user.getBio())
                .portfolioLinks(user.getPortfolioLinks())
                .companyName(user.getCompanyName())
                .ratingAvg(user.getRatingAvg())
                .ratingCount(user.getRatingCount())
                .walletBalance(user.getWalletBalance())
                .isBanned(user.getIsBanned())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

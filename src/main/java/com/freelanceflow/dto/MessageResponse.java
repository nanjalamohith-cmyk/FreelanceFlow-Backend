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
public class MessageResponse {
    private String id;
    private String contractId;
    private String senderId;
    private String senderName;
    private String content;
    private Instant createdAt;
}

package com.freelanceflow.dto;

import com.freelanceflow.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String id;
    private String userId;
    private String type;
    private String message;
    private Boolean read;
    private Instant createdAt;

    public static NotificationResponse fromNotification(Notification n) {
        if (n == null) return null;
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .type(n.getType())
                .message(n.getMessage())
                .read(n.getRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

package com.freelanceflow.controller;

import com.freelanceflow.dto.NotificationResponse;
import com.freelanceflow.security.UserPrincipal;
import com.freelanceflow.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notifications and live alerts")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications for logged in user")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getUserNotifications(principal.getId()));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark single notification as read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        notificationService.markAsRead(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.noContent().build();
    }
}

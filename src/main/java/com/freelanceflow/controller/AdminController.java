package com.freelanceflow.controller;

import com.freelanceflow.dto.BanUserRequest;
import com.freelanceflow.dto.DisputeResponse;
import com.freelanceflow.dto.PlatformStatsResponse;
import com.freelanceflow.dto.ResolveDisputeRequest;
import com.freelanceflow.dto.UserSummaryResponse;
import com.freelanceflow.security.UserPrincipal;
import com.freelanceflow.service.AdminService;
import com.freelanceflow.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative oversight, platform metrics, dispute resolution, and moderation")
public class AdminController {

    private final AdminService adminService;
    private final DisputeService disputeService;

    @GetMapping("/stats")
    @Operation(summary = "Get platform metrics and statistics (Admin only)")
    public ResponseEntity<PlatformStatsResponse> getPlatformStats() {
        return ResponseEntity.ok(adminService.getPlatformStats());
    }

    @GetMapping("/users")
    @Operation(summary = "List all registered platform users (Admin only)")
    public ResponseEntity<List<UserSummaryResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PatchMapping("/users/{userId}/ban")
    @Operation(summary = "Ban or unban a user (Admin only)")
    public ResponseEntity<UserSummaryResponse> banUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String userId,
            @Valid @RequestBody BanUserRequest request) {
        return ResponseEntity.ok(adminService.banUser(principal.getId(), userId, request.getIsBanned()));
    }

    @GetMapping("/disputes")
    @Operation(summary = "List all disputes queue (Admin only)")
    public ResponseEntity<List<DisputeResponse>> getAllDisputes() {
        return ResponseEntity.ok(disputeService.getAllDisputes());
    }

    @PostMapping("/disputes/{disputeId}/resolve")
    @Operation(summary = "Resolve a dispute with release, refund, or split (Admin only)")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String disputeId,
            @Valid @RequestBody ResolveDisputeRequest request) {
        return ResponseEntity.ok(disputeService.resolveDispute(principal.getId(), disputeId, request));
    }
}

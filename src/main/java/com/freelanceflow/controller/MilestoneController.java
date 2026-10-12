package com.freelanceflow.controller;

import com.freelanceflow.dto.MilestoneResponse;
import com.freelanceflow.dto.RequestRevisionRequest;
import com.freelanceflow.dto.SubmitDeliverableRequest;
import com.freelanceflow.model.Deliverable;
import com.freelanceflow.model.Milestone;
import com.freelanceflow.security.UserPrincipal;
import com.freelanceflow.service.MilestoneStateMachine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/milestones")
@RequiredArgsConstructor
@Tag(name = "Milestones", description = "Milestone escrow state transitions, deliverables submission, and fund release")
public class MilestoneController {

    private final MilestoneStateMachine milestoneStateMachine;

    @PostMapping("/{id}/fund")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    @Operation(summary = "Fund milestone: moves funds from client wallet into escrow (Client only)")
    public ResponseEntity<MilestoneResponse> fundMilestone(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        Milestone milestone = milestoneStateMachine.fundMilestone(principal.getId(), id);
        return ResponseEntity.ok(MilestoneResponse.fromMilestone(milestone));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('ADMIN')")
    @Operation(summary = "Start working on funded milestone (Freelancer only)")
    public ResponseEntity<MilestoneResponse> startMilestone(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        Milestone milestone = milestoneStateMachine.startMilestone(principal.getId(), id);
        return ResponseEntity.ok(MilestoneResponse.fromMilestone(milestone));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('ADMIN')")
    @Operation(summary = "Submit deliverable for milestone review (Freelancer only)")
    public ResponseEntity<MilestoneResponse> submitDeliverable(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody SubmitDeliverableRequest request) {
        Deliverable deliverable = Deliverable.builder()
                .fileName(request.getFileName())
                .url(request.getUrl())
                .uploadedAt(Instant.now())
                .build();
        Milestone milestone = milestoneStateMachine.submitDeliverable(principal.getId(), id, deliverable);
        return ResponseEntity.ok(MilestoneResponse.fromMilestone(milestone));
    }

    @PostMapping("/{id}/revision")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    @Operation(summary = "Request revisions with feedback note (Client only)")
    public ResponseEntity<MilestoneResponse> requestRevision(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody RequestRevisionRequest request) {
        Milestone milestone = milestoneStateMachine.requestRevision(principal.getId(), id, request.getNote());
        return ResponseEntity.ok(MilestoneResponse.fromMilestone(milestone));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    @Operation(summary = "Approve deliverable and release escrow funds to freelancer wallet (Client only)")
    public ResponseEntity<MilestoneResponse> approveMilestone(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        Milestone milestone = milestoneStateMachine.approveAndRelease(principal.getId(), id);
        return ResponseEntity.ok(MilestoneResponse.fromMilestone(milestone));
    }
}

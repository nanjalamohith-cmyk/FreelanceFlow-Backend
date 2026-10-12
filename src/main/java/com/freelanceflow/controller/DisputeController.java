package com.freelanceflow.controller;

import com.freelanceflow.dto.DisputeResponse;
import com.freelanceflow.dto.RaiseDisputeRequest;
import com.freelanceflow.security.UserPrincipal;
import com.freelanceflow.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Disputes", description = "Dispute initiation and viewing")
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping("/milestones/{milestoneId}/disputes")
    @Operation(summary = "Raise a dispute on a funded or in-progress milestone")
    public ResponseEntity<DisputeResponse> raiseDispute(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String milestoneId,
            @Valid @RequestBody RaiseDisputeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disputeService.raiseDispute(principal.getId(), milestoneId, request));
    }

    @GetMapping("/contracts/{contractId}/disputes")
    @Operation(summary = "Get all disputes associated with a contract")
    public ResponseEntity<List<DisputeResponse>> getContractDisputes(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String contractId) {
        return ResponseEntity.ok(disputeService.getContractDisputes(principal.getId(), contractId));
    }
}

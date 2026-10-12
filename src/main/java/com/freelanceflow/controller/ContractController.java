package com.freelanceflow.controller;

import com.freelanceflow.dto.AcceptBidRequest;
import com.freelanceflow.dto.ContractResponse;
import com.freelanceflow.security.UserPrincipal;
import com.freelanceflow.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@Tag(name = "Contracts", description = "Contract generation, milestones breakdown, and tracking")
public class ContractController {

    private final ContractService contractService;

    @PostMapping("/accept-bid/{jobId}/{bidId}")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    @Operation(summary = "Accept a freelancer bid and create a contract with defined milestones (Client only)")
    public ResponseEntity<ContractResponse> acceptBid(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String jobId,
            @PathVariable String bidId,
            @Valid @RequestBody AcceptBidRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contractService.acceptBidAndCreateContract(principal.getId(), jobId, bidId, request));
    }

    @GetMapping
    @Operation(summary = "Get list of all contracts for the current logged-in user")
    public ResponseEntity<List<ContractResponse>> getMyContracts(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(contractService.getUserContracts(principal.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full contract details including all milestones and timeline")
    public ResponseEntity<ContractResponse> getContractById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return ResponseEntity.ok(contractService.getContractById(principal.getId(), id));
    }
}

package com.freelanceflow.controller;

import com.freelanceflow.dto.BidResponse;
import com.freelanceflow.dto.CreateBidRequest;
import com.freelanceflow.security.UserPrincipal;
import com.freelanceflow.service.BidService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Bids", description = "Freelancer bid submission, proposal comparison, and withdrawal")
public class BidController {

    private final BidService bidService;

    @PostMapping("/jobs/{jobId}/bids")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('ADMIN')")
    @Operation(summary = "Submit a proposal and bid on an OPEN job (Freelancer only)")
    public ResponseEntity<BidResponse> createBid(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String jobId,
            @Valid @RequestBody CreateBidRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bidService.createBid(principal.getId(), jobId, request));
    }

    @GetMapping("/jobs/{jobId}/bids")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    @Operation(summary = "View bids submitted on a job (Job owner client only)")
    public ResponseEntity<List<BidResponse>> getBidsForJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String jobId) {
        return ResponseEntity.ok(bidService.getBidsForJob(principal.getId(), jobId));
    }

    @DeleteMapping("/bids/{bidId}")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('ADMIN')")
    @Operation(summary = "Withdraw a pending bid (Freelancer only)")
    public ResponseEntity<BidResponse> withdrawBid(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bidId) {
        return ResponseEntity.ok(bidService.withdrawBid(principal.getId(), bidId));
    }

    @GetMapping("/bids/my")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('ADMIN')")
    @Operation(summary = "View all bids submitted by the logged in freelancer")
    public ResponseEntity<List<BidResponse>> getMyBids(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bidService.getFreelancerBids(principal.getId()));
    }
}

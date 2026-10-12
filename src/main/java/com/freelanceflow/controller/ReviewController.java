package com.freelanceflow.controller;

import com.freelanceflow.dto.CreateReviewRequest;
import com.freelanceflow.dto.ReviewResponse;
import com.freelanceflow.security.UserPrincipal;
import com.freelanceflow.service.ReviewService;
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
@Tag(name = "Reviews", description = "Post-contract completion reviews and rating evaluations")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/contracts/{contractId}/reviews")
    @Operation(summary = "Submit a review and 1-5 star rating for completed contract")
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String contractId,
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(principal.getId(), contractId, request));
    }

    @GetMapping("/contracts/{contractId}/reviews")
    @Operation(summary = "Get reviews for a contract")
    public ResponseEntity<List<ReviewResponse>> getContractReviews(@PathVariable String contractId) {
        return ResponseEntity.ok(reviewService.getReviewsForContract(contractId));
    }

    @GetMapping("/users/{userId}/reviews")
    @Operation(summary = "Get all reviews received by a user")
    public ResponseEntity<List<ReviewResponse>> getUserReviews(@PathVariable String userId) {
        return ResponseEntity.ok(reviewService.getReviewsForUser(userId));
    }
}

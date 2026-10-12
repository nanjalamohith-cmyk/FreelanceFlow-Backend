package com.freelanceflow.service;

import com.freelanceflow.dto.CreateReviewRequest;
import com.freelanceflow.dto.ReviewResponse;
import com.freelanceflow.exception.ConflictException;
import com.freelanceflow.exception.ForbiddenException;
import com.freelanceflow.exception.ResourceNotFoundException;
import com.freelanceflow.model.Contract;
import com.freelanceflow.model.ContractStatus;
import com.freelanceflow.model.Review;
import com.freelanceflow.model.User;
import com.freelanceflow.repository.ContractRepository;
import com.freelanceflow.repository.ReviewRepository;
import com.freelanceflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public ReviewResponse createReview(String reviewerId, String contractId, CreateReviewRequest request) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        if (contract.getStatus() != ContractStatus.COMPLETED) {
            throw new ConflictException("Reviews can only be submitted after contract is COMPLETED");
        }

        boolean isClient = contract.getClientId().equals(reviewerId);
        boolean isFreelancer = contract.getFreelancerId().equals(reviewerId);

        if (!isClient && !isFreelancer) {
            throw new ForbiddenException("You are not a participant in this contract");
        }

        if (reviewRepository.existsByContractIdAndReviewerId(contractId, reviewerId)) {
            throw new ConflictException("You have already reviewed this contract");
        }

        String revieweeId = isClient ? contract.getFreelancerId() : contract.getClientId();
        User reviewee = userRepository.findById(revieweeId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewee user not found"));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer user not found"));

        Review review = Review.builder()
                .contractId(contractId)
                .reviewerId(reviewerId)
                .revieweeId(revieweeId)
                .rating(request.getRating())
                .comment(request.getComment().trim())
                .createdAt(Instant.now())
                .build();
        Review saved = reviewRepository.save(review);

        // Update reviewee's average rating and ratingCount
        int oldCount = reviewee.getRatingCount() != null ? reviewee.getRatingCount() : 0;
        double oldAvg = reviewee.getRatingAvg() != null ? reviewee.getRatingAvg() : 0.0;
        int newCount = oldCount + 1;
        double newAvg = ((oldAvg * oldCount) + request.getRating()) / newCount;
        double roundedAvg = BigDecimal.valueOf(newAvg).setScale(1, RoundingMode.HALF_UP).doubleValue();

        reviewee.setRatingCount(newCount);
        reviewee.setRatingAvg(roundedAvg);
        userRepository.save(reviewee);

        notificationService.createNotification(
                revieweeId,
                "NEW_REVIEW",
                reviewer.getName() + " left you a " + request.getRating() + "-star review!"
        );

        return toReviewResponse(saved, reviewer.getName(), reviewee.getName());
    }

    public List<ReviewResponse> getReviewsForUser(String userId) {
        return reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId).stream()
                .map(r -> {
                    User reviewer = userRepository.findById(r.getReviewerId()).orElse(null);
                    User reviewee = userRepository.findById(r.getRevieweeId()).orElse(null);
                    return toReviewResponse(r, reviewer != null ? reviewer.getName() : "Unknown",
                            reviewee != null ? reviewee.getName() : "Unknown");
                })
                .toList();
    }

    public List<ReviewResponse> getReviewsForContract(String contractId) {
        return reviewRepository.findByContractId(contractId).stream()
                .map(r -> {
                    User reviewer = userRepository.findById(r.getReviewerId()).orElse(null);
                    User reviewee = userRepository.findById(r.getRevieweeId()).orElse(null);
                    return toReviewResponse(r, reviewer != null ? reviewer.getName() : "Unknown",
                            reviewee != null ? reviewee.getName() : "Unknown");
                })
                .toList();
    }

    private ReviewResponse toReviewResponse(Review r, String reviewerName, String revieweeName) {
        return ReviewResponse.builder()
                .id(r.getId())
                .contractId(r.getContractId())
                .reviewerId(r.getReviewerId())
                .reviewerName(reviewerName)
                .revieweeId(r.getRevieweeId())
                .revieweeName(revieweeName)
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}

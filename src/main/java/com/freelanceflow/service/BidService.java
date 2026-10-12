package com.freelanceflow.service;

import com.freelanceflow.dto.BidResponse;
import com.freelanceflow.dto.CreateBidRequest;
import com.freelanceflow.exception.ConflictException;
import com.freelanceflow.exception.ForbiddenException;
import com.freelanceflow.exception.ResourceNotFoundException;
import com.freelanceflow.model.Bid;
import com.freelanceflow.model.BidStatus;
import com.freelanceflow.model.Job;
import com.freelanceflow.model.JobStatus;
import com.freelanceflow.model.Role;
import com.freelanceflow.model.User;
import com.freelanceflow.repository.BidRepository;
import com.freelanceflow.repository.JobRepository;
import com.freelanceflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public BidResponse createBid(String freelancerId, String jobId, CreateBidRequest request) {
        User freelancer = userRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));

        if (freelancer.getRole() != Role.FREELANCER && freelancer.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only freelancers can place bids");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new ConflictException("Bids can only be submitted for OPEN jobs");
        }

        if (bidRepository.existsByJobIdAndFreelancerId(jobId, freelancerId)) {
            throw new ConflictException("You have already submitted a bid for this job");
        }

        Bid bid = Bid.builder()
                .jobId(jobId)
                .freelancerId(freelancerId)
                .amount(request.getAmount())
                .deliveryDays(request.getDeliveryDays())
                .proposal(request.getProposal().trim())
                .status(BidStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        Bid saved = bidRepository.save(bid);

        // Alert client about the new bid
        notificationService.createNotification(
                job.getClientId(),
                "NEW_BID",
                freelancer.getName() + " placed a bid of $" + request.getAmount() + " on your job: " + job.getTitle()
        );

        return toBidResponse(saved, freelancer, job.getTitle());
    }

    public BidResponse withdrawBid(String freelancerId, String bidId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found: " + bidId));

        if (!bid.getFreelancerId().equals(freelancerId)) {
            throw new ForbiddenException("You can only withdraw your own bids");
        }

        if (bid.getStatus() != BidStatus.PENDING) {
            throw new ConflictException("Only PENDING bids can be withdrawn");
        }

        bid.setStatus(BidStatus.WITHDRAWN);
        Bid updated = bidRepository.save(bid);

        User freelancer = userRepository.findById(freelancerId).orElse(null);
        Job job = jobRepository.findById(bid.getJobId()).orElse(null);

        return toBidResponse(updated, freelancer, job != null ? job.getTitle() : "Unknown");
    }

    public List<BidResponse> getBidsForJob(String clientId, String jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        User user = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only the owner client or admin can view all bids on a job
        if (!job.getClientId().equals(clientId) && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You can only view bids on your own jobs");
        }

        return bidRepository.findByJobIdOrderByCreatedAtDesc(jobId).stream()
                .map(bid -> {
                    User freelancer = userRepository.findById(bid.getFreelancerId()).orElse(null);
                    return toBidResponse(bid, freelancer, job.getTitle());
                })
                .toList();
    }

    public List<BidResponse> getFreelancerBids(String freelancerId) {
        User freelancer = userRepository.findById(freelancerId).orElse(null);

        return bidRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancerId).stream()
                .map(bid -> {
                    Job job = jobRepository.findById(bid.getJobId()).orElse(null);
                    return toBidResponse(bid, freelancer, job != null ? job.getTitle() : "Unknown");
                })
                .toList();
    }

    public BidResponse toBidResponse(Bid bid, User freelancer, String jobTitle) {
        return BidResponse.builder()
                .id(bid.getId())
                .jobId(bid.getJobId())
                .jobTitle(jobTitle)
                .freelancerId(bid.getFreelancerId())
                .freelancerName(freelancer != null ? freelancer.getName() : "Unknown")
                .freelancerRatingAvg(freelancer != null ? freelancer.getRatingAvg() : 0.0)
                .freelancerRatingCount(freelancer != null ? freelancer.getRatingCount() : 0)
                .amount(bid.getAmount())
                .deliveryDays(bid.getDeliveryDays())
                .proposal(bid.getProposal())
                .status(bid.getStatus())
                .createdAt(bid.getCreatedAt())
                .build();
    }
}

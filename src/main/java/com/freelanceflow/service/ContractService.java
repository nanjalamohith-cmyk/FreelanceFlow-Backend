package com.freelanceflow.service;

import com.freelanceflow.dto.AcceptBidRequest;
import com.freelanceflow.dto.ContractResponse;
import com.freelanceflow.dto.MilestoneDefinitionDto;
import com.freelanceflow.dto.MilestoneResponse;
import com.freelanceflow.exception.BadRequestException;
import com.freelanceflow.exception.ConflictException;
import com.freelanceflow.exception.ForbiddenException;
import com.freelanceflow.exception.ResourceNotFoundException;
import com.freelanceflow.model.*;
import com.freelanceflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final MilestoneRepository milestoneRepository;
    private final JobRepository jobRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public ContractResponse acceptBidAndCreateContract(String clientId, String jobId, String bidId, AcceptBidRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (!job.getClientId().equals(clientId)) {
            throw new ForbiddenException("Only the job creator can accept bids");
        }

        if (job.getStatus() != JobStatus.OPEN) {
            throw new ConflictException("Job is not open for bid acceptance (current status: " + job.getStatus() + ")");
        }

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found: " + bidId));

        if (!bid.getJobId().equals(jobId)) {
            throw new BadRequestException("Bid does not belong to this job");
        }

        if (bid.getStatus() != BidStatus.PENDING) {
            throw new ConflictException("Only PENDING bids can be accepted");
        }

        if (request.getMilestones() == null || request.getMilestones().isEmpty()) {
            throw new BadRequestException("At least one milestone must be provided");
        }

        // Validate milestone sum matches bid total amount
        BigDecimal totalMilestones = request.getMilestones().stream()
                .map(MilestoneDefinitionDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalMilestones.compareTo(bid.getAmount()) != 0) {
            throw new BadRequestException(
                    "Total sum of milestones ($" + totalMilestones + ") must exactly match the bid amount ($" + bid.getAmount() + ")");
        }

        // 1. Create contract
        Contract contract = Contract.builder()
                .jobId(jobId)
                .clientId(clientId)
                .freelancerId(bid.getFreelancerId())
                .totalAmount(bid.getAmount())
                .status(ContractStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
        Contract savedContract = contractRepository.save(contract);

        // 2. Create milestones
        List<Milestone> createdMilestones = new ArrayList<>();
        int orderIndex = 1;
        for (MilestoneDefinitionDto dto : request.getMilestones()) {
            Milestone m = Milestone.builder()
                    .contractId(savedContract.getId())
                    .title(dto.getTitle().trim())
                    .description(dto.getDescription())
                    .amount(dto.getAmount())
                    .dueDate(dto.getDueDate())
                    .order(dto.getOrder() != null ? dto.getOrder() : orderIndex++)
                    .status(MilestoneStatus.PENDING_FUNDING)
                    .deliverables(new ArrayList<>())
                    .build();
            createdMilestones.add(milestoneRepository.save(m));
        }

        // 3. Mark accepted bid
        bid.setStatus(BidStatus.ACCEPTED);
        bidRepository.save(bid);

        // 4. Reject all other bids on this job
        List<Bid> otherBids = bidRepository.findByJobIdOrderByCreatedAtDesc(jobId);
        for (Bid other : otherBids) {
            if (!other.getId().equals(bidId) && other.getStatus() == BidStatus.PENDING) {
                other.setStatus(BidStatus.REJECTED);
                bidRepository.save(other);
            }
        }

        // 5. Update job status to IN_CONTRACT
        job.setStatus(JobStatus.IN_CONTRACT);
        jobRepository.save(job);

        // 6. Notify freelancer
        User client = userRepository.findById(clientId).orElse(null);
        notificationService.createNotification(
                bid.getFreelancerId(),
                "BID_ACCEPTED",
                "Congratulations! Your bid on '" + job.getTitle() + "' was accepted. Contract #" + savedContract.getId() + " is now active."
        );

        User freelancer = userRepository.findById(bid.getFreelancerId()).orElse(null);
        return toContractResponse(savedContract, job.getTitle(), client != null ? client.getName() : "Unknown",
                freelancer != null ? freelancer.getName() : "Unknown", createdMilestones);
    }

    public ContractResponse getContractById(String userId, String contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        boolean isParticipant = contract.getClientId().equals(userId) || contract.getFreelancerId().equals(userId);
        if (!isParticipant && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You are not a participant of this contract");
        }

        Job job = jobRepository.findById(contract.getJobId()).orElse(null);
        User client = userRepository.findById(contract.getClientId()).orElse(null);
        User freelancer = userRepository.findById(contract.getFreelancerId()).orElse(null);
        List<Milestone> milestones = milestoneRepository.findByContractIdOrderByOrderAsc(contractId);

        return toContractResponse(contract,
                job != null ? job.getTitle() : "Job #" + contract.getJobId(),
                client != null ? client.getName() : "Unknown",
                freelancer != null ? freelancer.getName() : "Unknown",
                milestones);
    }

    public List<ContractResponse> getUserContracts(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        List<Contract> contracts;
        if (user.getRole() == Role.ADMIN) {
            contracts = contractRepository.findAll();
        } else if (user.getRole() == Role.CLIENT) {
            contracts = contractRepository.findByClientIdOrderByCreatedAtDesc(userId);
        } else {
            contracts = contractRepository.findByFreelancerIdOrderByCreatedAtDesc(userId);
        }

        return contracts.stream().map(c -> {
            Job job = jobRepository.findById(c.getJobId()).orElse(null);
            User client = userRepository.findById(c.getClientId()).orElse(null);
            User freelancer = userRepository.findById(c.getFreelancerId()).orElse(null);
            List<Milestone> milestones = milestoneRepository.findByContractIdOrderByOrderAsc(c.getId());
            return toContractResponse(c,
                    job != null ? job.getTitle() : "Job",
                    client != null ? client.getName() : "Unknown",
                    freelancer != null ? freelancer.getName() : "Unknown",
                    milestones);
        }).toList();
    }

    public ContractResponse toContractResponse(Contract contract, String jobTitle, String clientName,
                                               String freelancerName, List<Milestone> milestones) {
        List<MilestoneResponse> milestoneResponses = milestones.stream()
                .map(MilestoneResponse::fromMilestone)
                .toList();

        return ContractResponse.builder()
                .id(contract.getId())
                .jobId(contract.getJobId())
                .jobTitle(jobTitle)
                .clientId(contract.getClientId())
                .clientName(clientName)
                .freelancerId(contract.getFreelancerId())
                .freelancerName(freelancerName)
                .totalAmount(contract.getTotalAmount())
                .status(contract.getStatus())
                .milestones(milestoneResponses)
                .createdAt(contract.getCreatedAt())
                .build();
    }
}

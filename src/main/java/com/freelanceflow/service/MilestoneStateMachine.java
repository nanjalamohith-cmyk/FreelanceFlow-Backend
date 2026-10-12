package com.freelanceflow.service;

import com.freelanceflow.exception.ForbiddenException;
import com.freelanceflow.exception.IllegalStateTransitionException;
import com.freelanceflow.exception.ResourceNotFoundException;
import com.freelanceflow.model.*;
import com.freelanceflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilestoneStateMachine {

    private final MilestoneRepository milestoneRepository;
    private final ContractRepository contractRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final DisputeRepository disputeRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;

    // Allowed state transitions map
    private static final Map<MilestoneStatus, Set<MilestoneStatus>> ALLOWED_TRANSITIONS = Map.of(
            MilestoneStatus.PENDING_FUNDING, Set.of(MilestoneStatus.FUNDED),
            MilestoneStatus.FUNDED, Set.of(MilestoneStatus.IN_PROGRESS, MilestoneStatus.DISPUTED),
            MilestoneStatus.IN_PROGRESS, Set.of(MilestoneStatus.SUBMITTED, MilestoneStatus.DISPUTED),
            MilestoneStatus.SUBMITTED, Set.of(MilestoneStatus.APPROVED, MilestoneStatus.RELEASED, MilestoneStatus.REVISION_REQUESTED, MilestoneStatus.DISPUTED),
            MilestoneStatus.REVISION_REQUESTED, Set.of(MilestoneStatus.IN_PROGRESS, MilestoneStatus.SUBMITTED, MilestoneStatus.DISPUTED),
            MilestoneStatus.DISPUTED, Set.of(MilestoneStatus.RELEASED)
    );

    public void validateTransition(MilestoneStatus current, MilestoneStatus target) {
        Set<MilestoneStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Collections.emptySet());
        if (!allowed.contains(target)) {
            throw new IllegalStateTransitionException(
                    "Illegal transition from " + current + " to " + target + ". Allowed targets: " + allowed
            );
        }
    }

    /**
     * PENDING_FUNDING -> FUNDED
     * Triggered by CLIENT
     */
    @Transactional
    public Milestone fundMilestone(String userId, String milestoneId) {
        Milestone milestone = getMilestone(milestoneId);
        Contract contract = getContract(milestone.getContractId());

        if (!contract.getClientId().equals(userId)) {
            throw new ForbiddenException("Only the contract client can fund this milestone");
        }

        validateTransition(milestone.getStatus(), MilestoneStatus.FUNDED);

        User client = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + userId));

        // Atomic wallet movement: deduct from client wallet & record ESCROW_HOLD ledger
        walletService.recordEscrowHold(client, contract.getId(), milestone.getId(), milestone.getAmount());

        milestone.setStatus(MilestoneStatus.FUNDED);
        Milestone saved = milestoneRepository.save(milestone);

        notificationService.createNotification(
                contract.getFreelancerId(),
                "MILESTONE_FUNDED",
                "Milestone '" + milestone.getTitle() + "' ($" + milestone.getAmount() + ") has been funded. You may start work."
        );

        return saved;
    }

    /**
     * FUNDED or REVISION_REQUESTED -> IN_PROGRESS
     * Triggered by FREELANCER
     */
    @Transactional
    public Milestone startMilestone(String userId, String milestoneId) {
        Milestone milestone = getMilestone(milestoneId);
        Contract contract = getContract(milestone.getContractId());

        if (!contract.getFreelancerId().equals(userId)) {
            throw new ForbiddenException("Only the assigned freelancer can start this milestone");
        }

        validateTransition(milestone.getStatus(), MilestoneStatus.IN_PROGRESS);

        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        Milestone saved = milestoneRepository.save(milestone);

        notificationService.createNotification(
                contract.getClientId(),
                "MILESTONE_IN_PROGRESS",
                "Freelancer has started work on milestone '" + milestone.getTitle() + "'."
        );

        return saved;
    }

    /**
     * IN_PROGRESS (or FUNDED/REVISION_REQUESTED) -> SUBMITTED
     * Triggered by FREELANCER
     */
    @Transactional
    public Milestone submitDeliverable(String userId, String milestoneId, Deliverable deliverable) {
        Milestone milestone = getMilestone(milestoneId);
        Contract contract = getContract(milestone.getContractId());

        if (!contract.getFreelancerId().equals(userId)) {
            throw new ForbiddenException("Only the assigned freelancer can submit deliverables for this milestone");
        }

        validateTransition(milestone.getStatus(), MilestoneStatus.SUBMITTED);

        if (deliverable != null) {
            if (milestone.getDeliverables() == null) {
                milestone.setDeliverables(new ArrayList<>());
            }
            milestone.getDeliverables().add(deliverable);
        }

        milestone.setStatus(MilestoneStatus.SUBMITTED);
        Milestone saved = milestoneRepository.save(milestone);

        notificationService.createNotification(
                contract.getClientId(),
                "MILESTONE_SUBMITTED",
                "Deliverables submitted for milestone '" + milestone.getTitle() + "'. Please review and approve."
        );

        return saved;
    }

    /**
     * SUBMITTED -> REVISION_REQUESTED
     * Triggered by CLIENT
     */
    @Transactional
    public Milestone requestRevision(String userId, String milestoneId, String revisionNote) {
        Milestone milestone = getMilestone(milestoneId);
        Contract contract = getContract(milestone.getContractId());

        if (!contract.getClientId().equals(userId)) {
            throw new ForbiddenException("Only the contract client can request revisions");
        }

        validateTransition(milestone.getStatus(), MilestoneStatus.REVISION_REQUESTED);

        milestone.setStatus(MilestoneStatus.REVISION_REQUESTED);
        milestone.setRevisionNote(revisionNote);
        Milestone saved = milestoneRepository.save(milestone);

        notificationService.createNotification(
                contract.getFreelancerId(),
                "MILESTONE_REVISION_REQUESTED",
                "Revision requested for milestone '" + milestone.getTitle() + "': " + revisionNote
        );

        return saved;
    }

    /**
     * SUBMITTED -> APPROVED -> RELEASED
     * Triggered by CLIENT
     */
    @Transactional
    public Milestone approveAndRelease(String userId, String milestoneId) {
        Milestone milestone = getMilestone(milestoneId);
        Contract contract = getContract(milestone.getContractId());

        if (!contract.getClientId().equals(userId)) {
            throw new ForbiddenException("Only the contract client can approve and release funds");
        }

        validateTransition(milestone.getStatus(), MilestoneStatus.RELEASED);

        User freelancer = userRepository.findById(contract.getFreelancerId())
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));

        // Atomic wallet movement: release escrow into freelancer wallet & record RELEASE ledger
        walletService.recordRelease(freelancer, contract.getId(), milestone.getId(), milestone.getAmount());

        milestone.setStatus(MilestoneStatus.RELEASED);
        Milestone saved = milestoneRepository.save(milestone);

        notificationService.createNotification(
                contract.getFreelancerId(),
                "MILESTONE_RELEASED",
                "Milestone '" + milestone.getTitle() + "' approved! $" + milestone.getAmount() + " released to your wallet."
        );

        checkAndCompleteContract(contract);

        return saved;
    }

    /**
     * Any funded state -> DISPUTED
     * Triggered by either CLIENT or FREELANCER
     */
    @Transactional
    public Dispute raiseDispute(String userId, String milestoneId, String reason) {
        Milestone milestone = getMilestone(milestoneId);
        Contract contract = getContract(milestone.getContractId());

        boolean isClient = contract.getClientId().equals(userId);
        boolean isFreelancer = contract.getFreelancerId().equals(userId);

        if (!isClient && !isFreelancer) {
            throw new ForbiddenException("Only contract participants can raise a dispute");
        }

        validateTransition(milestone.getStatus(), MilestoneStatus.DISPUTED);

        milestone.setStatus(MilestoneStatus.DISPUTED);
        milestoneRepository.save(milestone);

        contract.setStatus(ContractStatus.DISPUTED);
        contractRepository.save(contract);

        Dispute dispute = Dispute.builder()
                .contractId(contract.getId())
                .milestoneId(milestone.getId())
                .raisedBy(userId)
                .reason(reason)
                .status(DisputeStatus.OPEN)
                .createdAt(Instant.now())
                .build();
        Dispute savedDispute = disputeRepository.save(dispute);

        String otherPartyId = isClient ? contract.getFreelancerId() : contract.getClientId();
        notificationService.createNotification(
                otherPartyId,
                "DISPUTE_OPENED",
                "A dispute was raised for milestone '" + milestone.getTitle() + "'. Admin review is pending."
        );

        return savedDispute;
    }

    /**
     * DISPUTED -> RELEASED (or refunded)
     * Triggered by ADMIN
     */
    @Transactional
    public Dispute resolveDispute(String adminId, String disputeId, DisputeResolution resolution,
                                  Double splitPercentToFreelancer, String adminNote) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        if (admin.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only platform administrators can resolve disputes");
        }

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found: " + disputeId));

        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new IllegalStateTransitionException("Dispute is already resolved");
        }

        Milestone milestone = getMilestone(dispute.getMilestoneId());
        Contract contract = getContract(dispute.getContractId());

        if (milestone.getStatus() != MilestoneStatus.DISPUTED) {
            throw new IllegalStateTransitionException("Milestone is not in DISPUTED state");
        }

        User client = userRepository.findById(contract.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        User freelancer = userRepository.findById(contract.getFreelancerId())
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));

        BigDecimal milestoneAmount = milestone.getAmount();

        switch (resolution) {
            case RELEASE_TO_FREELANCER:
                walletService.recordRelease(freelancer, contract.getId(), milestone.getId(), milestoneAmount);
                milestone.setStatus(MilestoneStatus.RELEASED);
                break;

            case REFUND_TO_CLIENT:
                walletService.recordRefund(client, contract.getId(), milestone.getId(), milestoneAmount);
                milestone.setStatus(MilestoneStatus.RELEASED); // Handled / settled
                break;

            case SPLIT:
                double splitPercent = (splitPercentToFreelancer != null) ? splitPercentToFreelancer : 50.0;
                if (splitPercent < 0 || splitPercent > 100) {
                    throw new IllegalStateTransitionException("Split percentage must be between 0 and 100");
                }
                BigDecimal freelancerRatio = BigDecimal.valueOf(splitPercent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                BigDecimal freelancerAmount = milestoneAmount.multiply(freelancerRatio).setScale(2, RoundingMode.HALF_UP);
                BigDecimal clientAmount = milestoneAmount.subtract(freelancerAmount);

                if (freelancerAmount.compareTo(BigDecimal.ZERO) > 0) {
                    walletService.recordRelease(freelancer, contract.getId(), milestone.getId(), freelancerAmount);
                }
                if (clientAmount.compareTo(BigDecimal.ZERO) > 0) {
                    walletService.recordRefund(client, contract.getId(), milestone.getId(), clientAmount);
                }
                milestone.setStatus(MilestoneStatus.RELEASED);
                break;
        }

        milestoneRepository.save(milestone);

        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolution(resolution);
        dispute.setSplitPercentToFreelancer(splitPercentToFreelancer);
        dispute.setAdminNote(adminNote);
        Dispute savedDispute = disputeRepository.save(dispute);

        // Reset contract status if no other active disputes
        contract.setStatus(ContractStatus.ACTIVE);
        contractRepository.save(contract);
        checkAndCompleteContract(contract);

        notificationService.createNotification(
                contract.getClientId(),
                "DISPUTE_RESOLVED",
                "Dispute on milestone '" + milestone.getTitle() + "' resolved by admin: " + resolution
        );
        notificationService.createNotification(
                contract.getFreelancerId(),
                "DISPUTE_RESOLVED",
                "Dispute on milestone '" + milestone.getTitle() + "' resolved by admin: " + resolution
        );

        return savedDispute;
    }

    private void checkAndCompleteContract(Contract contract) {
        List<Milestone> milestones = milestoneRepository.findByContractIdOrderByOrderAsc(contract.getId());
        boolean allReleased = !milestones.isEmpty() && milestones.stream()
                .allMatch(m -> m.getStatus() == MilestoneStatus.RELEASED);

        if (allReleased) {
            contract.setStatus(ContractStatus.COMPLETED);
            contractRepository.save(contract);

            jobRepository.findById(contract.getJobId()).ifPresent(job -> {
                job.setStatus(JobStatus.COMPLETED);
                jobRepository.save(job);
            });

            notificationService.createNotification(
                    contract.getClientId(),
                    "CONTRACT_COMPLETED",
                    "All milestones released! Contract #" + contract.getId() + " is now completed. Please leave a review."
            );
            notificationService.createNotification(
                    contract.getFreelancerId(),
                    "CONTRACT_COMPLETED",
                    "All milestones released! Contract #" + contract.getId() + " is now completed. Please leave a review."
            );
        }
    }

    public Milestone getMilestone(String milestoneId) {
        return milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found: " + milestoneId));
    }

    public Contract getContract(String contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));
    }
}

package com.freelanceflow;

import com.freelanceflow.exception.ForbiddenException;
import com.freelanceflow.exception.IllegalStateTransitionException;
import com.freelanceflow.exception.InsufficientFundsException;
import com.freelanceflow.model.*;
import com.freelanceflow.repository.*;
import com.freelanceflow.service.MilestoneStateMachine;
import com.freelanceflow.service.NotificationService;
import com.freelanceflow.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MilestoneStateMachineTest {

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MilestoneStateMachine stateMachine;

    private User client;
    private User freelancer;
    private User admin;
    private Job job;
    private Contract contract;
    private Milestone milestone;

    @BeforeEach
    void setUp() {
        client = User.builder()
                .id("client-123")
                .name("Sarah Client")
                .email("client@test.com")
                .role(Role.CLIENT)
                .walletBalance(BigDecimal.valueOf(1000.00))
                .build();

        freelancer = User.builder()
                .id("freelancer-456")
                .name("Alex Freelancer")
                .email("freelancer@test.com")
                .role(Role.FREELANCER)
                .walletBalance(BigDecimal.valueOf(200.00))
                .build();

        admin = User.builder()
                .id("admin-789")
                .name("Admin User")
                .email("admin@test.com")
                .role(Role.ADMIN)
                .build();

        job = Job.builder()
                .id("job-001")
                .clientId("client-123")
                .title("Full Stack Web App")
                .status(JobStatus.IN_CONTRACT)
                .build();

        contract = Contract.builder()
                .id("contract-001")
                .jobId("job-001")
                .clientId("client-123")
                .freelancerId("freelancer-456")
                .totalAmount(BigDecimal.valueOf(500.00))
                .status(ContractStatus.ACTIVE)
                .build();

        milestone = Milestone.builder()
                .id("milestone-001")
                .contractId("contract-001")
                .title("Phase 1: Architecture & Backend")
                .amount(BigDecimal.valueOf(500.00))
                .status(MilestoneStatus.PENDING_FUNDING)
                .deliverables(new ArrayList<>())
                .order(1)
                .build();
    }

    @Test
    @DisplayName("1. Valid Full Flow: PENDING_FUNDING -> FUNDED -> IN_PROGRESS -> SUBMITTED -> RELEASED & Completes Contract")
    void testValidFullFlow() {
        // Step 1: Fund Milestone (Client)
        when(milestoneRepository.findById("milestone-001")).thenReturn(Optional.of(milestone));
        when(contractRepository.findById("contract-001")).thenReturn(Optional.of(contract));
        when(userRepository.findById("client-123")).thenReturn(Optional.of(client));
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));

        Milestone funded = stateMachine.fundMilestone("client-123", "milestone-001");
        assertEquals(MilestoneStatus.FUNDED, funded.getStatus());
        verify(walletService).recordEscrowHold(eq(client), eq("contract-001"), eq("milestone-001"), eq(BigDecimal.valueOf(500.00)));

        // Step 2: Freelancer starts milestone
        Milestone started = stateMachine.startMilestone("freelancer-456", "milestone-001");
        assertEquals(MilestoneStatus.IN_PROGRESS, started.getStatus());

        // Step 3: Freelancer submits deliverable
        Deliverable deliverable = Deliverable.builder()
                .fileName("solution.zip")
                .url("/api/files/solution.zip")
                .uploadedAt(Instant.now())
                .build();
        Milestone submitted = stateMachine.submitDeliverable("freelancer-456", "milestone-001", deliverable);
        assertEquals(MilestoneStatus.SUBMITTED, submitted.getStatus());
        assertEquals(1, submitted.getDeliverables().size());

        // Step 4: Client approves and releases escrow
        when(userRepository.findById("freelancer-456")).thenReturn(Optional.of(freelancer));
        when(milestoneRepository.findByContractIdOrderByOrderAsc("contract-001")).thenReturn(List.of(milestone));
        when(jobRepository.findById("job-001")).thenReturn(Optional.of(job));

        Milestone released = stateMachine.approveAndRelease("client-123", "milestone-001");
        assertEquals(MilestoneStatus.RELEASED, released.getStatus());
        verify(walletService).recordRelease(eq(freelancer), eq("contract-001"), eq("milestone-001"), eq(BigDecimal.valueOf(500.00)));

        // Contract & Job should automatically transition to COMPLETED
        assertEquals(ContractStatus.COMPLETED, contract.getStatus());
        assertEquals(JobStatus.COMPLETED, job.getStatus());
    }

    @Test
    @DisplayName("2. Illegal Transition Rejected with HTTP 409 (IllegalStateTransitionException)")
    void testIllegalTransitionRejected() {
        // Trying to skip from PENDING_FUNDING directly to SUBMITTED or RELEASED
        when(milestoneRepository.findById("milestone-001")).thenReturn(Optional.of(milestone));
        when(contractRepository.findById("contract-001")).thenReturn(Optional.of(contract));

        // Attempting to submit while milestone is still PENDING_FUNDING
        assertThrows(IllegalStateTransitionException.class, () -> {
            stateMachine.submitDeliverable("freelancer-456", "milestone-001", null);
        });

        // Attempting to approve while milestone is still PENDING_FUNDING
        assertThrows(IllegalStateTransitionException.class, () -> {
            stateMachine.approveAndRelease("client-123", "milestone-001");
        });
    }

    @Test
    @DisplayName("3. Wrong-Role Rejected with HTTP 403 (ForbiddenException)")
    void testWrongRoleRejected() {
        when(milestoneRepository.findById("milestone-001")).thenReturn(Optional.of(milestone));
        when(contractRepository.findById("contract-001")).thenReturn(Optional.of(contract));

        // Freelancer attempts to fund the milestone (only Client can fund)
        assertThrows(ForbiddenException.class, () -> {
            stateMachine.fundMilestone("freelancer-456", "milestone-001");
        });

        // Client attempts to start milestone (only Freelancer can start)
        milestone.setStatus(MilestoneStatus.FUNDED);
        assertThrows(ForbiddenException.class, () -> {
            stateMachine.startMilestone("client-123", "milestone-001");
        });

        // Client attempts to submit deliverable (only Freelancer can submit)
        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        assertThrows(ForbiddenException.class, () -> {
            stateMachine.submitDeliverable("client-123", "milestone-001", null);
        });
    }

    @Test
    @DisplayName("4. Dispute Split: Milestone DISPUTED resolved by Admin with 60/40 Split")
    void testDisputeSplit() {
        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        when(milestoneRepository.findById("milestone-001")).thenReturn(Optional.of(milestone));
        when(contractRepository.findById("contract-001")).thenReturn(Optional.of(contract));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(i -> {
            Dispute d = i.getArgument(0);
            d.setId("dispute-001");
            return d;
        });

        // Freelancer raises dispute
        Dispute raised = stateMachine.raiseDispute("freelancer-456", "milestone-001", "Scope creep disagreement");
        assertEquals(MilestoneStatus.DISPUTED, milestone.getStatus());
        assertEquals(ContractStatus.DISPUTED, contract.getStatus());
        assertEquals(DisputeStatus.OPEN, raised.getStatus());

        // Admin resolves dispute with SPLIT (60% to freelancer, 40% to client)
        when(userRepository.findById("admin-789")).thenReturn(Optional.of(admin));
        when(userRepository.findById("client-123")).thenReturn(Optional.of(client));
        when(userRepository.findById("freelancer-456")).thenReturn(Optional.of(freelancer));
        when(disputeRepository.findById("dispute-001")).thenReturn(Optional.of(raised));
        when(milestoneRepository.findByContractIdOrderByOrderAsc("contract-001")).thenReturn(List.of(milestone));

        Dispute resolved = stateMachine.resolveDispute(
                "admin-789",
                "dispute-001",
                DisputeResolution.SPLIT,
                60.0,
                "Fair compromise: 60% paid for delivered work, 40% refunded to client."
        );

        assertEquals(DisputeStatus.RESOLVED, resolved.getStatus());
        assertEquals(DisputeResolution.SPLIT, resolved.getResolution());
        assertEquals(MilestoneStatus.RELEASED, milestone.getStatus());

        // Total milestone is $500: 60% = $300.00 released to freelancer, 40% = $200.00 refunded to client
        verify(walletService).recordRelease(eq(freelancer), eq("contract-001"), eq("milestone-001"), eq(BigDecimal.valueOf(300.00).setScale(2)));
        verify(walletService).recordRefund(eq(client), eq("contract-001"), eq("milestone-001"), eq(BigDecimal.valueOf(200.00).setScale(2)));
    }

    @Test
    @DisplayName("5. Insufficient Wallet Balance Throws InsufficientFundsException")
    void testInsufficientWalletBalance() {
        when(milestoneRepository.findById("milestone-001")).thenReturn(Optional.of(milestone));
        when(contractRepository.findById("contract-001")).thenReturn(Optional.of(contract));
        when(userRepository.findById("client-123")).thenReturn(Optional.of(client));

        // WalletService throws InsufficientFundsException if balance < amount
        doThrow(new InsufficientFundsException("Insufficient wallet balance. Milestone requires $500.00 but available balance is $100.00"))
                .when(walletService).recordEscrowHold(any(), any(), any(), any());

        assertThrows(InsufficientFundsException.class, () -> {
            stateMachine.fundMilestone("client-123", "milestone-001");
        });
    }
}

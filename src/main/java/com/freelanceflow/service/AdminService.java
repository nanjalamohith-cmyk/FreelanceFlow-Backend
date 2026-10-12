package com.freelanceflow.service;

import com.freelanceflow.dto.PlatformStatsResponse;
import com.freelanceflow.dto.UserSummaryResponse;
import com.freelanceflow.exception.BadRequestException;
import com.freelanceflow.exception.ResourceNotFoundException;
import com.freelanceflow.model.*;
import com.freelanceflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ContractRepository contractRepository;
    private final DisputeRepository disputeRepository;
    private final TransactionRepository transactionRepository;

    public PlatformStatsResponse getPlatformStats() {
        long totalUsers = userRepository.count();
        long totalClients = userRepository.countByRole(Role.CLIENT);
        long totalFreelancers = userRepository.countByRole(Role.FREELANCER);
        long totalJobs = jobRepository.count();
        long activeContracts = contractRepository.countByStatus(ContractStatus.ACTIVE);
        long completedContracts = contractRepository.countByStatus(ContractStatus.COMPLETED);
        long openDisputes = disputeRepository.countByStatus(DisputeStatus.OPEN);

        // Sum escrow volume (all ESCROW_HOLD transactions)
        BigDecimal totalEscrowVolume = transactionRepository.findByType(TransactionType.ESCROW_HOLD).stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PlatformStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalClients(totalClients)
                .totalFreelancers(totalFreelancers)
                .totalJobs(totalJobs)
                .activeContracts(activeContracts)
                .completedContracts(completedContracts)
                .totalEscrowVolume(totalEscrowVolume)
                .openDisputes(openDisputes)
                .build();
    }

    public List<UserSummaryResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserSummaryResponse::fromUser)
                .toList();
    }

    public UserSummaryResponse banUser(String adminId, String targetUserId, boolean isBanned) {
        if (adminId.equals(targetUserId)) {
            throw new BadRequestException("Admin cannot ban themselves");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + targetUserId));

        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Admin users cannot be banned");
        }

        user.setIsBanned(isBanned);
        User saved = userRepository.save(user);
        return UserSummaryResponse.fromUser(saved);
    }
}

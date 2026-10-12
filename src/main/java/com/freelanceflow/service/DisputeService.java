package com.freelanceflow.service;

import com.freelanceflow.dto.DisputeResponse;
import com.freelanceflow.dto.RaiseDisputeRequest;
import com.freelanceflow.dto.ResolveDisputeRequest;
import com.freelanceflow.exception.ForbiddenException;
import com.freelanceflow.exception.ResourceNotFoundException;
import com.freelanceflow.model.*;
import com.freelanceflow.repository.ContractRepository;
import com.freelanceflow.repository.DisputeRepository;
import com.freelanceflow.repository.MilestoneRepository;
import com.freelanceflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final MilestoneRepository milestoneRepository;
    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final MilestoneStateMachine milestoneStateMachine;

    public DisputeResponse raiseDispute(String userId, String milestoneId, RaiseDisputeRequest request) {
        Dispute dispute = milestoneStateMachine.raiseDispute(userId, milestoneId, request.getReason());
        return enrichDisputeResponse(dispute);
    }

    public DisputeResponse resolveDispute(String adminId, String disputeId, ResolveDisputeRequest request) {
        Dispute dispute = milestoneStateMachine.resolveDispute(
                adminId,
                disputeId,
                request.getResolution(),
                request.getSplitPercentToFreelancer(),
                request.getAdminNote()
        );
        return enrichDisputeResponse(dispute);
    }

    public List<DisputeResponse> getAllDisputes() {
        return disputeRepository.findAll().stream()
                .map(this::enrichDisputeResponse)
                .toList();
    }

    public List<DisputeResponse> getContractDisputes(String userId, String contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isParticipant = contract.getClientId().equals(userId) || contract.getFreelancerId().equals(userId);
        if (!isParticipant && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Access denied");
        }

        return disputeRepository.findByContractId(contractId).stream()
                .map(this::enrichDisputeResponse)
                .toList();
    }

    private DisputeResponse enrichDisputeResponse(Dispute dispute) {
        DisputeResponse resp = DisputeResponse.fromDispute(dispute);
        userRepository.findById(dispute.getRaisedBy()).ifPresent(u -> resp.setRaisedByName(u.getName()));
        milestoneRepository.findById(dispute.getMilestoneId()).ifPresent(m -> {
            resp.setMilestoneTitle(m.getTitle());
            resp.setMilestoneAmount(m.getAmount());
        });
        return resp;
    }
}

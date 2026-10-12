package com.freelanceflow.service;

import com.freelanceflow.dto.MessageResponse;
import com.freelanceflow.exception.ForbiddenException;
import com.freelanceflow.exception.ResourceNotFoundException;
import com.freelanceflow.model.Contract;
import com.freelanceflow.model.Message;
import com.freelanceflow.model.Role;
import com.freelanceflow.model.User;
import com.freelanceflow.repository.ContractRepository;
import com.freelanceflow.repository.MessageRepository;
import com.freelanceflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageResponse sendMessage(String senderId, String contractId, String content) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + senderId));

        boolean isParticipant = contract.getClientId().equals(senderId) || contract.getFreelancerId().equals(senderId);
        if (!isParticipant && sender.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only contract participants can send messages");
        }

        Message message = Message.builder()
                .contractId(contractId)
                .senderId(senderId)
                .content(content.trim())
                .createdAt(Instant.now())
                .build();
        Message saved = messageRepository.save(message);

        MessageResponse response = MessageResponse.builder()
                .id(saved.getId())
                .contractId(saved.getContractId())
                .senderId(saved.getSenderId())
                .senderName(sender.getName())
                .content(saved.getContent())
                .createdAt(saved.getCreatedAt())
                .build();

        // Broadcast to WebSocket topic /topic/contract/{id}
        try {
            messagingTemplate.convertAndSend("/topic/contract/" + contractId, response);
        } catch (Exception e) {
            log.warn("Failed to broadcast message to contract {}: {}", contractId, e.getMessage());
        }

        return response;
    }

    public List<MessageResponse> getContractMessages(String userId, String contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        boolean isParticipant = contract.getClientId().equals(userId) || contract.getFreelancerId().equals(userId);
        if (!isParticipant && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Access denied");
        }

        return messageRepository.findByContractIdOrderByCreatedAtAsc(contractId).stream()
                .map(m -> {
                    User sender = userRepository.findById(m.getSenderId()).orElse(null);
                    return MessageResponse.builder()
                            .id(m.getId())
                            .contractId(m.getContractId())
                            .senderId(m.getSenderId())
                            .senderName(sender != null ? sender.getName() : "Unknown")
                            .content(m.getContent())
                            .createdAt(m.getCreatedAt())
                            .build();
                })
                .toList();
    }
}

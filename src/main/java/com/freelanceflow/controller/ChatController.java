package com.freelanceflow.controller;

import com.freelanceflow.dto.MessageResponse;
import com.freelanceflow.dto.SendMessageRequest;
import com.freelanceflow.security.UserPrincipal;
import com.freelanceflow.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts/{contractId}/messages")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Per-contract chat and messaging between participants")
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    @Operation(summary = "Get chat history for a contract")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String contractId) {
        return ResponseEntity.ok(chatService.getContractMessages(principal.getId(), contractId));
    }

    @PostMapping
    @Operation(summary = "Send a chat message in a contract")
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String contractId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(principal.getId(), contractId, request.getContent()));
    }
}

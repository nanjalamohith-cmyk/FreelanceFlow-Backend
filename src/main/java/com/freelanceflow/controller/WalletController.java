package com.freelanceflow.controller;

import com.freelanceflow.dto.AddFundsRequest;
import com.freelanceflow.dto.WalletResponse;
import com.freelanceflow.model.Transaction;
import com.freelanceflow.security.UserPrincipal;
import com.freelanceflow.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Simulated escrow wallet, top-up funds, balance, and transaction history")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "Get user wallet balance and recent transactions")
    public ResponseEntity<WalletResponse> getWallet(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(walletService.getWallet(principal.getId()));
    }

    @PostMapping("/add-funds")
    @Operation(summary = "Simulated top-up to add funds to wallet")
    public ResponseEntity<WalletResponse> addFunds(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddFundsRequest request) {
        return ResponseEntity.ok(walletService.addFunds(principal.getId(), request.getAmount()));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get full transaction ledger for current user")
    public ResponseEntity<List<Transaction>> getTransactions(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(walletService.getUserTransactions(principal.getId()));
    }
}

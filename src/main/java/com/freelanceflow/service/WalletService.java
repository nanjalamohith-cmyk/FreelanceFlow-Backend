package com.freelanceflow.service;

import com.freelanceflow.dto.WalletResponse;
import com.freelanceflow.exception.BadRequestException;
import com.freelanceflow.exception.InsufficientFundsException;
import com.freelanceflow.exception.ResourceNotFoundException;
import com.freelanceflow.model.Transaction;
import com.freelanceflow.model.TransactionType;
import com.freelanceflow.model.User;
import com.freelanceflow.repository.TransactionRepository;
import com.freelanceflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public WalletResponse addFunds(String userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        BigDecimal currentBalance = user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = currentBalance.add(amount);
        user.setWalletBalance(newBalance);
        userRepository.save(user);

        Transaction tx = Transaction.builder()
                .userId(userId)
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .balanceAfter(newBalance)
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(tx);

        log.info("User {} deposited ${}. New balance: ${}", userId, amount, newBalance);

        return getWallet(userId);
    }

    public WalletResponse getWallet(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        List<Transaction> transactions = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return WalletResponse.builder()
                .balance(user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO)
                .transactions(transactions)
                .build();
    }

    public List<Transaction> getUserTransactions(String userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Transaction recordEscrowHold(User client, String contractId, String milestoneId, BigDecimal amount) {
        BigDecimal balance = client.getWalletBalance() != null ? client.getWalletBalance() : BigDecimal.ZERO;
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient wallet balance. Milestone requires $" + amount + " but available balance is $" + balance);
        }

        BigDecimal newBalance = balance.subtract(amount);
        client.setWalletBalance(newBalance);
        userRepository.save(client);

        Transaction tx = Transaction.builder()
                .userId(client.getId())
                .contractId(contractId)
                .milestoneId(milestoneId)
                .type(TransactionType.ESCROW_HOLD)
                .amount(amount)
                .balanceAfter(newBalance)
                .createdAt(Instant.now())
                .build();
        return transactionRepository.save(tx);
    }

    @Transactional
    public Transaction recordRelease(User freelancer, String contractId, String milestoneId, BigDecimal amount) {
        BigDecimal currentBalance = freelancer.getWalletBalance() != null ? freelancer.getWalletBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = currentBalance.add(amount);
        freelancer.setWalletBalance(newBalance);
        userRepository.save(freelancer);

        Transaction tx = Transaction.builder()
                .userId(freelancer.getId())
                .contractId(contractId)
                .milestoneId(milestoneId)
                .type(TransactionType.RELEASE)
                .amount(amount)
                .balanceAfter(newBalance)
                .createdAt(Instant.now())
                .build();
        return transactionRepository.save(tx);
    }

    @Transactional
    public Transaction recordRefund(User client, String contractId, String milestoneId, BigDecimal amount) {
        BigDecimal currentBalance = client.getWalletBalance() != null ? client.getWalletBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = currentBalance.add(amount);
        client.setWalletBalance(newBalance);
        userRepository.save(client);

        Transaction tx = Transaction.builder()
                .userId(client.getId())
                .contractId(contractId)
                .milestoneId(milestoneId)
                .type(TransactionType.REFUND)
                .amount(amount)
                .balanceAfter(newBalance)
                .createdAt(Instant.now())
                .build();
        return transactionRepository.save(tx);
    }
}

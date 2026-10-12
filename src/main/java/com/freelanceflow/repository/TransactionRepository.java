package com.freelanceflow.repository;

import com.freelanceflow.model.Transaction;
import com.freelanceflow.model.TransactionType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Transaction> findByContractId(String contractId);
    List<Transaction> findByType(TransactionType type);
}

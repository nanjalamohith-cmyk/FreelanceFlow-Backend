package com.freelanceflow.repository;

import com.freelanceflow.model.Dispute;
import com.freelanceflow.model.DisputeStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisputeRepository extends MongoRepository<Dispute, String> {
    List<Dispute> findByContractId(String contractId);
    Optional<Dispute> findByMilestoneId(String milestoneId);
    List<Dispute> findByStatusOrderByCreatedAtDesc(DisputeStatus status);
    long countByStatus(DisputeStatus status);
}

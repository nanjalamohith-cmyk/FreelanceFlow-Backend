package com.freelanceflow.repository;

import com.freelanceflow.model.Milestone;
import com.freelanceflow.model.MilestoneStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MilestoneRepository extends MongoRepository<Milestone, String> {
    List<Milestone> findByContractIdOrderByOrderAsc(String contractId);
    long countByContractId(String contractId);
    long countByContractIdAndStatusNot(String contractId, MilestoneStatus status);
}

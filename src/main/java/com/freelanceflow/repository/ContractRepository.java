package com.freelanceflow.repository;

import com.freelanceflow.model.Contract;
import com.freelanceflow.model.ContractStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends MongoRepository<Contract, String> {
    List<Contract> findByClientIdOrderByCreatedAtDesc(String clientId);
    List<Contract> findByFreelancerIdOrderByCreatedAtDesc(String freelancerId);
    Optional<Contract> findByJobId(String jobId);
    long countByStatus(ContractStatus status);
}

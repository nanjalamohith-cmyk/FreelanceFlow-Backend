package com.freelanceflow.repository;

import com.freelanceflow.model.Bid;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends MongoRepository<Bid, String> {
    List<Bid> findByJobIdOrderByCreatedAtDesc(String jobId);
    List<Bid> findByFreelancerIdOrderByCreatedAtDesc(String freelancerId);
    Optional<Bid> findByJobIdAndFreelancerId(String jobId, String freelancerId);
    boolean existsByJobIdAndFreelancerId(String jobId, String freelancerId);
}

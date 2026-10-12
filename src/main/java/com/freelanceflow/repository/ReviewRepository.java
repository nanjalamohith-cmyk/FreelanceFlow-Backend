package com.freelanceflow.repository;

import com.freelanceflow.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByRevieweeIdOrderByCreatedAtDesc(String revieweeId);
    List<Review> findByContractId(String contractId);
    boolean existsByContractIdAndReviewerId(String contractId, String reviewerId);
}

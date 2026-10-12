package com.freelanceflow.repository;

import com.freelanceflow.model.Job;
import com.freelanceflow.model.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends MongoRepository<Job, String> {
    List<Job> findByClientIdOrderByCreatedAtDesc(String clientId);
    Page<Job> findByStatus(JobStatus status, Pageable pageable);
    long countByStatus(JobStatus status);
}

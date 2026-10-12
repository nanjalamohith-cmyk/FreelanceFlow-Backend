package com.freelanceflow.service;

import com.freelanceflow.dto.CreateJobRequest;
import com.freelanceflow.dto.JobResponse;
import com.freelanceflow.dto.UpdateJobRequest;
import com.freelanceflow.exception.ForbiddenException;
import com.freelanceflow.exception.ResourceNotFoundException;
import com.freelanceflow.model.Job;
import com.freelanceflow.model.JobStatus;
import com.freelanceflow.model.Role;
import com.freelanceflow.model.User;
import com.freelanceflow.repository.BidRepository;
import com.freelanceflow.repository.JobRepository;
import com.freelanceflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final BidRepository bidRepository;
    private final MongoTemplate mongoTemplate;

    public JobResponse createJob(String clientId, CreateJobRequest request) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (client.getRole() != Role.CLIENT && client.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only clients can post jobs");
        }

        Job job = Job.builder()
                .clientId(clientId)
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .budget(request.getBudget())
                .skills(request.getSkills() != null ? request.getSkills() : new ArrayList<>())
                .deadline(request.getDeadline())
                .status(JobStatus.OPEN)
                .createdAt(Instant.now())
                .build();

        Job saved = jobRepository.save(job);
        return toJobResponse(saved, client.getName());
    }

    public JobResponse updateJob(String clientId, String jobId, UpdateJobRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (!job.getClientId().equals(clientId)) {
            throw new ForbiddenException("You can only edit your own jobs");
        }

        if (job.getStatus() != JobStatus.OPEN) {
            throw new ForbiddenException("Only OPEN jobs can be edited");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            job.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            job.setDescription(request.getDescription().trim());
        }
        if (request.getBudget() != null) {
            job.setBudget(request.getBudget());
        }
        if (request.getSkills() != null) {
            job.setSkills(request.getSkills());
        }
        if (request.getDeadline() != null) {
            job.setDeadline(request.getDeadline());
        }

        Job updated = jobRepository.save(job);
        User client = userRepository.findById(clientId).orElse(null);
        return toJobResponse(updated, client != null ? client.getName() : "Unknown");
    }

    public JobResponse cancelJob(String clientId, String jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (!job.getClientId().equals(clientId)) {
            throw new ForbiddenException("You can only cancel your own jobs");
        }

        if (job.getStatus() != JobStatus.OPEN) {
            throw new ForbiddenException("Only OPEN jobs can be cancelled");
        }

        job.setStatus(JobStatus.CANCELLED);
        Job updated = jobRepository.save(job);
        User client = userRepository.findById(clientId).orElse(null);
        return toJobResponse(updated, client != null ? client.getName() : "Unknown");
    }

    public JobResponse getJobById(String jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        User client = userRepository.findById(job.getClientId()).orElse(null);
        return toJobResponse(job, client != null ? client.getName() : "Unknown");
    }

    public List<JobResponse> getClientJobs(String clientId) {
        User client = userRepository.findById(clientId).orElse(null);
        String clientName = client != null ? client.getName() : "Unknown";

        return jobRepository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(job -> toJobResponse(job, clientName))
                .toList();
    }

    public Page<JobResponse> searchJobs(String keyword, String skill, BigDecimal minBudget, BigDecimal maxBudget, JobStatus status, Pageable pageable) {
        Query query = new Query();

        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        if (keyword != null && !keyword.isBlank()) {
            Criteria keywordCriteria = new Criteria().orOperator(
                    Criteria.where("title").regex(keyword.trim(), "i"),
                    Criteria.where("description").regex(keyword.trim(), "i")
            );
            query.addCriteria(keywordCriteria);
        }

        if (skill != null && !skill.isBlank()) {
            query.addCriteria(Criteria.where("skills").regex(skill.trim(), "i"));
        }

        if (minBudget != null && maxBudget != null) {
            query.addCriteria(Criteria.where("budget").gte(minBudget).lte(maxBudget));
        } else if (minBudget != null) {
            query.addCriteria(Criteria.where("budget").gte(minBudget));
        } else if (maxBudget != null) {
            query.addCriteria(Criteria.where("budget").lte(maxBudget));
        }

        long total = mongoTemplate.count(query, Job.class);
        query.with(pageable);
        List<Job> jobs = mongoTemplate.find(query, Job.class);

        List<JobResponse> responseList = jobs.stream().map(job -> {
            User client = userRepository.findById(job.getClientId()).orElse(null);
            return toJobResponse(job, client != null ? client.getName() : "Unknown");
        }).toList();

        return new PageImpl<>(responseList, pageable, total);
    }

    public JobResponse toJobResponse(Job job, String clientName) {
        long bidCount = bidRepository.findByJobIdOrderByCreatedAtDesc(job.getId()).size();
        return JobResponse.builder()
                .id(job.getId())
                .clientId(job.getClientId())
                .clientName(clientName)
                .title(job.getTitle())
                .description(job.getDescription())
                .budget(job.getBudget())
                .skills(job.getSkills())
                .deadline(job.getDeadline())
                .status(job.getStatus())
                .bidCount(bidCount)
                .createdAt(job.getCreatedAt())
                .build();
    }
}

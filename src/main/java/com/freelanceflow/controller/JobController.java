package com.freelanceflow.controller;

import com.freelanceflow.dto.CreateJobRequest;
import com.freelanceflow.dto.JobResponse;
import com.freelanceflow.dto.UpdateJobRequest;
import com.freelanceflow.model.JobStatus;
import com.freelanceflow.security.UserPrincipal;
import com.freelanceflow.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job postings, searching, filtering, and management")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    @Operation(summary = "Post a new job (Client only)")
    public ResponseEntity<JobResponse> createJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateJobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(principal.getId(), request));
    }

    @GetMapping
    @Operation(summary = "Browse and search jobs with filters and pagination")
    public ResponseEntity<Page<JobResponse>> getJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) BigDecimal minBudget,
            @RequestParam(required = false) BigDecimal maxBudget,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(jobService.searchJobs(keyword, skill, minBudget, maxBudget, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information for a specific job")
    public ResponseEntity<JobResponse> getJobById(@PathVariable String id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    @Operation(summary = "Get all jobs posted by the logged in client")
    public ResponseEntity<List<JobResponse>> getMyJobs(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(jobService.getClientJobs(principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    @Operation(summary = "Edit an existing OPEN job")
    public ResponseEntity<JobResponse> updateJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @RequestBody UpdateJobRequest request) {
        return ResponseEntity.ok(jobService.updateJob(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    @Operation(summary = "Cancel an OPEN job")
    public ResponseEntity<JobResponse> cancelJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return ResponseEntity.ok(jobService.cancelJob(principal.getId(), id));
    }
}

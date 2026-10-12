package com.freelanceflow.config;

import com.freelanceflow.model.Job;
import com.freelanceflow.model.JobStatus;
import com.freelanceflow.model.Role;
import com.freelanceflow.model.User;
import com.freelanceflow.repository.JobRepository;
import com.freelanceflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        ensureIndexes();
        seedAdminUser();
        seedDemoData();
    }

    private void ensureIndexes() {
        try {
            // users.email (unique)
            mongoTemplate.indexOps("users").ensureIndex(
                    new Index().on("email", Sort.Direction.ASC).unique()
            );

            // jobs.status
            mongoTemplate.indexOps("jobs").ensureIndex(
                    new Index().on("status", Sort.Direction.ASC)
            );

            // bids (jobId + freelancerId unique)
            mongoTemplate.indexOps("bids").ensureIndex(
                    new Index().on("jobId", Sort.Direction.ASC)
                            .on("freelancerId", Sort.Direction.ASC)
                            .unique()
            );

            // messages.contractId
            mongoTemplate.indexOps("messages").ensureIndex(
                    new Index().on("contractId", Sort.Direction.ASC)
            );

            log.info("MongoDB indexes verified successfully.");
        } catch (Exception e) {
            log.warn("Non-fatal: Could not verify indexes: {}", e.getMessage());
        }
    }

    private void seedAdminUser() {
        final String adminEmail = "admin@freelanceflow.dev";
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .name("Platform Administrator")
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode("Admin@12345"))
                    .role(Role.ADMIN)
                    .skills(List.of("Platform Management", "Dispute Resolution", "Escrow Auditing"))
                    .bio("FreelanceFlow System Administrator")
                    .walletBalance(BigDecimal.valueOf(10000.00))
                    .ratingAvg(5.0)
                    .ratingCount(0)
                    .isBanned(false)
                    .createdAt(Instant.now())
                    .build();
            userRepository.save(admin);
            log.info("Seeded initial ADMIN user: {} / Admin@12345", adminEmail);
        }
    }

    private void seedDemoData() {
        final String clientEmail = "client@freelanceflow.dev";
        User client;
        if (!userRepository.existsByEmail(clientEmail)) {
            client = User.builder()
                    .name("Sarah Connor (Acme Tech)")
                    .email(clientEmail)
                    .passwordHash(passwordEncoder.encode("Client@12345"))
                    .role(Role.CLIENT)
                    .companyName("Acme Technologies Inc.")
                    .bio("Engineering Director building next-generation cloud solutions.")
                    .walletBalance(BigDecimal.valueOf(5000.00))
                    .ratingAvg(4.9)
                    .ratingCount(8)
                    .isBanned(false)
                    .createdAt(Instant.now())
                    .build();
            client = userRepository.save(client);
            log.info("Seeded initial CLIENT user: {} / Client@12345", clientEmail);
        } else {
            client = userRepository.findByEmail(clientEmail).orElse(null);
        }

        final String freelancerEmail = "freelancer@freelanceflow.dev";
        if (!userRepository.existsByEmail(freelancerEmail)) {
            User freelancer = User.builder()
                    .name("Alex Rivera")
                    .email(freelancerEmail)
                    .passwordHash(passwordEncoder.encode("Freelancer@12345"))
                    .role(Role.FREELANCER)
                    .skills(List.of("React", "TypeScript", "Node.js", "Tailwind CSS", "Spring Boot", "MongoDB"))
                    .bio("Senior Full-Stack Engineer with 7+ years building enterprise SaaS and fintech web applications.")
                    .portfolioLinks(List.of("https://github.com/alexrivera", "https://alexrivera.dev"))
                    .walletBalance(BigDecimal.valueOf(750.00))
                    .ratingAvg(4.95)
                    .ratingCount(14)
                    .isBanned(false)
                    .createdAt(Instant.now())
                    .build();
            userRepository.save(freelancer);
            log.info("Seeded initial FREELANCER user: {} / Freelancer@12345", freelancerEmail);
        }

        // Seed sample jobs if empty
        if (client != null && jobRepository.count() == 0) {
            Job job1 = Job.builder()
                    .clientId(client.getId())
                    .title("Full Stack Marketplace Dashboard with React & Spring Boot")
                    .description("We are looking for a senior engineer to implement an interactive analytical dashboard for our marketplace. Needs clean responsive UI in Tailwind CSS, secure JWT authentication, and MongoDB backend.")
                    .budget(BigDecimal.valueOf(1800.00))
                    .skills(List.of("React", "Spring Boot", "Tailwind CSS", "MongoDB"))
                    .deadline(Instant.now().plus(14, ChronoUnit.DAYS))
                    .status(JobStatus.OPEN)
                    .createdAt(Instant.now())
                    .build();

            Job job2 = Job.builder()
                    .clientId(client.getId())
                    .title("Real-Time Notification & WebSocket Integration")
                    .description("Build a STOMP WebSocket notification pipeline with fallback polling, delivering real-time alerts on milestone state changes and chat messages.")
                    .budget(BigDecimal.valueOf(950.00))
                    .skills(List.of("WebSocket", "Java", "STOMP", "React"))
                    .deadline(Instant.now().plus(7, ChronoUnit.DAYS))
                    .status(JobStatus.OPEN)
                    .createdAt(Instant.now())
                    .build();

            Job job3 = Job.builder()
                    .clientId(client.getId())
                    .title("Escrow Smart Wallet & Transaction Ledger")
                    .description("Design a multi-currency escrow wallet simulator with immutable double-entry ledger records and dispute management.")
                    .budget(BigDecimal.valueOf(2400.00))
                    .skills(List.of("Spring Security", "Escrow", "MongoDB", "Fintech"))
                    .deadline(Instant.now().plus(21, ChronoUnit.DAYS))
                    .status(JobStatus.OPEN)
                    .createdAt(Instant.now())
                    .build();

            jobRepository.saveAll(List.of(job1, job2, job3));
            log.info("Seeded 3 sample open jobs.");
        }
    }
}

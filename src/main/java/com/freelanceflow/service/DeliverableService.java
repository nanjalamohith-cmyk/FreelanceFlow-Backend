package com.freelanceflow.service;

import com.freelanceflow.exception.BadRequestException;
import com.freelanceflow.model.Deliverable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class DeliverableService {

    private final Path uploadPath;

    public DeliverableService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException e) {
            log.error("Could not create upload directory", e);
        }
    }

    public Deliverable storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Failed to store empty file");
        }

        String originalFilename = file.getOriginalFilename();
        String safeName = (originalFilename != null) ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_") : "deliverable";
        String storedFilename = UUID.randomUUID() + "_" + safeName;

        try {
            Path targetLocation = this.uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String downloadUrl = "/api/files/" + storedFilename;
            return Deliverable.builder()
                    .fileName(safeName)
                    .url(downloadUrl)
                    .uploadedAt(Instant.now())
                    .build();
        } catch (IOException e) {
            log.error("Error storing deliverable file", e);
            throw new BadRequestException("Could not store file: " + e.getMessage());
        }
    }

    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = this.uploadPath.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new BadRequestException("File not found or not readable: " + filename);
            }
        } catch (Exception e) {
            throw new BadRequestException("File not found: " + filename);
        }
    }
}

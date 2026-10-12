package com.freelanceflow.controller;

import com.freelanceflow.model.Deliverable;
import com.freelanceflow.service.DeliverableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Deliverables", description = "Multipart file uploads and deliverable downloads")
public class DeliverableController {

    private final DeliverableService deliverableService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload milestone deliverable file")
    public ResponseEntity<Deliverable> uploadFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(deliverableService.storeFile(file));
    }

    @GetMapping("/{fileName:.+}")
    @Operation(summary = "Download deliverable file by filename")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        Resource resource = deliverableService.loadFileAsResource(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}

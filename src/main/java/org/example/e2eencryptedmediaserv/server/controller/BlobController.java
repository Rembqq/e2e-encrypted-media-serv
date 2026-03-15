package org.example.e2eencryptedmediaserv.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.e2eencryptedmediaserv.server.model.BlobMetadata;
import org.example.e2eencryptedmediaserv.server.model.User;
import org.example.e2eencryptedmediaserv.server.model.dto.BlobResponse;
import org.example.e2eencryptedmediaserv.server.model.dto.BlobUploadMetadata;
import org.example.e2eencryptedmediaserv.server.security.CustomUserDetails;
import org.example.e2eencryptedmediaserv.server.service.BlobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/blobs")
public class BlobController {

    private final BlobService service;
    private final ObjectMapper objectMapper;

    public BlobController(BlobService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<List<BlobResponse>> getUserBlobs(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = currentUser.getId();
        List<BlobResponse> blobs = service.getUserBlobs(userId);

        return ResponseEntity.ok(blobs);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestPart("blob") MultipartFile file,
            @RequestPart("metadata") String metadataJson,
            @AuthenticationPrincipal CustomUserDetails currentUser
            ) throws Exception {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }

        Long currentUserId = currentUser.getId();

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File part is required and must not be empty");
        }

        BlobUploadMetadata metadata;
        try {
            metadata = objectMapper.readValue(metadataJson, BlobUploadMetadata.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid metadata JSON format: " + e.getMessage());
        }

        List<String> errors = new ArrayList<>();

        // filename
        String realFilename = file.getOriginalFilename();
        if (realFilename == null || realFilename.trim().isEmpty()) {
            errors.add("Uploaded file must have a name");
        } else if (!realFilename.equals(metadata.originalFilename())) {
            errors.add("originalFilename mismatch: metadata says '" + metadata.originalFilename() + "', actual file name is '" + realFilename + "'");
        } else if (realFilename.contains("..") || realFilename.startsWith("/")) {
            errors.add("Invalid filename: potential path traversal");
        }

        // size
        if (metadata.size() == null || metadata.size() <= 0) {
            errors.add("size must be positive");
        } else if (metadata.size() != file.getSize()) {
            errors.add("Size mismatch: metadata says " + metadata.size() + " bytes, actual file size " + file.getSize() + " bytes");
        }

        // modifiedAt
        if (metadata.modifiedAt() == null) {
            errors.add("modifiedAt is required");
        } else if (metadata.modifiedAt().isAfter(Instant.now().plusSeconds(300))) {
            errors.add("modifiedAt cannot be in the future");
        }

        // cipherHash
        if (metadata.cipherHash() == null || metadata.cipherHash().trim().isEmpty()) {
            errors.add("cipherHash is required");
        } else if (!metadata.cipherHash().matches("^[0-9a-fA-F]{64}$")) { // пример для SHA-256
            errors.add("cipherHash must be a valid SHA-256 hex string (64 characters)");
        }

        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "details", errors
            ));
        }

        BlobMetadata saved =
                service.handleUpload(file.getInputStream(), metadata, currentUserId);

        Map<String, Object> response = Map.of(
                "blobId", saved.getId(),
                "storageKey", saved.getStorageKey(),
                "deduped", saved.getRefcount() > 1
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }
}

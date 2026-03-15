package org.example.e2eencryptedmediaserv.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record SnapshotFileRequest(
        @NotBlank String path,           // "/photos/2025/vacation/img_001.jpg"
        @NotBlank String blobId,         // "sha256-abc123..." или uuid
        @Positive Long size,
        @PastOrPresent Instant modifiedAt     // или String в ISO, клиент сам решает
        // можно добавить: String originalHash (опционально позже)
) {}

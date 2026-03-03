package org.example.e2eencryptedmediaserv.server.model.dto;

import java.time.Instant;

public record SnapshotFileRequest(
        String path,           // "/photos/2025/vacation/img_001.jpg"
        String blobId,         // "sha256-abc123..." или uuid
        Long size,
        Instant modifiedAt     // или String в ISO, клиент сам решает
        // можно добавить: String originalHash (опционально позже)
) {}

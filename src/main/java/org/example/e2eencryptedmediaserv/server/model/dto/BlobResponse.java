package org.example.e2eencryptedmediaserv.server.model.dto;

import java.time.Instant;
import java.util.UUID;

public record BlobResponse(
        UUID blobId,
        String originalFilename,
        Long size,
        Instant createdAt,
        String storageKey,
        Boolean deduped
) {}

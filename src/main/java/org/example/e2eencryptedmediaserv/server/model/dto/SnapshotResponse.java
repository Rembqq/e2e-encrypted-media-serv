package org.example.e2eencryptedmediaserv.server.model.dto;

import java.time.Instant;
import java.util.List;

public record SnapshotResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Long totalSize,
        Integer fileCount,
        List<SnapshotFileRequest> files
) {}
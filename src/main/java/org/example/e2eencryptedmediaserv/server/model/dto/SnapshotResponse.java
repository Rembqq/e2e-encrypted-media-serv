package org.example.e2eencryptedmediaserv.server.model.dto;

import java.time.Instant;

public record SnapshotResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Long totalSize,
        Integer fileCount
        // опционально: List<SnapshotFileResponse> files при детальном запросе
) {}
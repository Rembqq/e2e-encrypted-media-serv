package org.example.e2eencryptedmediaserv.server.model.dto;

import java.util.List;

public record SnapshotCreateRequest(
        String name,                    // "weekly-2025-02-26"
        String description,             // nullable
        List<SnapshotFileRequest> files
) {}

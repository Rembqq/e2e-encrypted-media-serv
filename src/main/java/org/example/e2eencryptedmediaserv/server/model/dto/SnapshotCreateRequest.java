package org.example.e2eencryptedmediaserv.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SnapshotCreateRequest(
        @NotBlank String name,
        String description,             // nullable
        @NotEmpty List<SnapshotFileRequest> files
) {}

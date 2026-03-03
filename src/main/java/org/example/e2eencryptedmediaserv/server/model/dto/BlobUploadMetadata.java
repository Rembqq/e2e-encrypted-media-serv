package org.example.e2eencryptedmediaserv.server.model.dto;

import java.time.Instant;

public record BlobUploadMetadata(
        String clientId,
        String originalFilename,
        Long size,
        Instant modifiedAt,
        String cipherHash
) {}

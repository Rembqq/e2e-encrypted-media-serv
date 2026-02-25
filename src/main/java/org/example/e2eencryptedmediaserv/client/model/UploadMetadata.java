package org.example.e2eencryptedmediaserv.client.model;

import java.time.Instant;

public record UploadMetadata(
        String clientId,
        String originalFilename,
        Long size,
        Instant modifiedAt,
        String cipherHash
) {}

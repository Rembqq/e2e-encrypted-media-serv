package org.example.e2eencryptedmediaserv.server.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blobs")
public class BlobMetadata {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String hash;

    @Column(nullable = false)
    private String storageKey;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false)
    private Integer refcount;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(columnDefinition = "jsonb")
    private String metadata;
}



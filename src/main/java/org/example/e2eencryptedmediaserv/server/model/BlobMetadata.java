package org.example.e2eencryptedmediaserv.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private BlobUploadMetadata metadata;
}



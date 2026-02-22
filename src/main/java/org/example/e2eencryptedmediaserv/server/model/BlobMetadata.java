package org.example.e2eencryptedmediaserv.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.e2eencryptedmediaserv.server.util.BlobUploadMetadataJsonConverter;
import org.hibernate.annotations.ColumnTransformer;

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

    @Convert(converter = BlobUploadMetadataJsonConverter.class)
    @Column(columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private BlobUploadMetadata metadata;
}



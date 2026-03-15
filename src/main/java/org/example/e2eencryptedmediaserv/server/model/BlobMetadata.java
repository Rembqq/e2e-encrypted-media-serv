    package org.example.e2eencryptedmediaserv.server.model;

    import jakarta.persistence.*;
    import lombok.Getter;
    import lombok.NoArgsConstructor;
    import lombok.Setter;
    import org.example.e2eencryptedmediaserv.server.model.dto.BlobUploadMetadata;
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

        @Column(name = "user_id", nullable = false)
        private Long userId;

        @Column(nullable = false)
        private String hash;

        @Column(nullable = false)
        private String storageKey;

        @Column(nullable = false)
        private Long size;

        @Column(nullable = false)
        private Integer refcount = 0;

        @Column(nullable = false)
        private Instant createdAt;

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(columnDefinition = "jsonb", nullable = false)
        private BlobUploadMetadata metadata;
    }



package org.example.e2eencryptedmediaserv.server.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "backup_files")
public class BackupFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private Snapshot snapshot;

    @Column(nullable = false)
    private String path;

    @Column(name = "blob_id", nullable = false)
    private UUID blobId;

    @Column
    private Long size;

    @Column(columnDefinition = "timestamptz")
    private Instant modifiedAt;

}

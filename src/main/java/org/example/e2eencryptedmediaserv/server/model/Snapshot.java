package org.example.e2eencryptedmediaserv.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;


@Entity
@Table(name = "snapshots")
@NoArgsConstructor
@Getter
@Setter
public class Snapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, columnDefinition = "timestamptz")
    private Instant createdAt = Instant.now();

    private Long totalSize;

    private Integer fileCount;
}


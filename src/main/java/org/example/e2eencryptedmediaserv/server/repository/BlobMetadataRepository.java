package org.example.e2eencryptedmediaserv.server.repository;

import org.example.e2eencryptedmediaserv.server.model.BlobMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlobMetadataRepository extends JpaRepository<BlobMetadata, UUID> {
    Optional<BlobMetadata> findByHash(String hash);
    boolean existsByHash(String hash);
}

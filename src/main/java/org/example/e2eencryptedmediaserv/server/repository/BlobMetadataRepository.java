package org.example.e2eencryptedmediaserv.server.repository;

import org.example.e2eencryptedmediaserv.server.model.BlobMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlobMetadataRepository extends JpaRepository<BlobMetadata, UUID> {
    List<BlobMetadata> findByUserId(Long userId);
    Optional<BlobMetadata> findByUserIdAndHash(Long userId, String hash);
}

package org.example.e2eencryptedmediaserv.server.repository;

import org.example.e2eencryptedmediaserv.server.model.Snapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SnapshotRepository extends JpaRepository<Snapshot, Long> {
    List<Snapshot> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Snapshot> findByIdAndUserId(Long id, Long userId);
}

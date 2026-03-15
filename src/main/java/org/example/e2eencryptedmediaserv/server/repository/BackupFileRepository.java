package org.example.e2eencryptedmediaserv.server.repository;

import org.example.e2eencryptedmediaserv.server.model.BackupFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BackupFileRepository extends JpaRepository<BackupFile, Long> {
    List<BackupFile> findBySnapshotIdAndUserId(Long snapshotId, Long userId);
}

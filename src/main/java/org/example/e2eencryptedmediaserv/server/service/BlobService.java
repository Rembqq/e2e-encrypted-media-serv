package org.example.e2eencryptedmediaserv.server.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.example.e2eencryptedmediaserv.server.model.BackupFile;
import org.example.e2eencryptedmediaserv.server.model.BlobMetadata;
import org.example.e2eencryptedmediaserv.server.model.Snapshot;
import org.example.e2eencryptedmediaserv.server.model.dto.BlobUploadMetadata;
import org.example.e2eencryptedmediaserv.server.model.dto.SnapshotCreateRequest;
import org.example.e2eencryptedmediaserv.server.model.dto.SnapshotFileRequest;
import org.example.e2eencryptedmediaserv.server.repository.BackupFileRepository;
import org.example.e2eencryptedmediaserv.server.repository.BlobMetadataRepository;
import org.example.e2eencryptedmediaserv.server.repository.SnapshotRepository;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BlobService {
    private final BlobMetadataRepository blobRepository;
    private final BlobStorage storage;
    private final SnapshotRepository snapshotRepository;
    private final BackupFileRepository backupFileRepository;

    public BlobService(BlobMetadataRepository blobRepository, BlobStorage storage, SnapshotRepository snapshotRepository, BackupFileRepository backupFileRepository) {
        this.blobRepository = blobRepository;
        this.storage = storage;
        this.snapshotRepository = snapshotRepository;
        this.backupFileRepository = backupFileRepository;
    }

    public void incrementRefCount(BlobMetadata meta) {
        meta.setRefcount(meta.getRefcount() + 1);
    }

    @Transactional
    public BlobMetadata handleUpload(InputStream stream, BlobUploadMetadata meta) {
        Optional<BlobMetadata> existing = blobRepository.findByHash(meta.cipherHash());

        if(existing.isPresent()) {
            BlobMetadata blob = existing.get();
            incrementRefCount(blob);
            return blobRepository.save(blob);
        }

        UUID id = UUID.randomUUID();

        String storageKey = storage.put(id, stream);

        BlobMetadata blob = new BlobMetadata();
        blob.setId(id);
        blob.setHash(meta.cipherHash());
        blob.setStorageKey(storageKey);
        blob.setSize(meta.size());
        blob.setRefcount(1);
        blob.setCreatedAt(Instant.now());
        blob.setMetadata(meta);

        return blobRepository.save(blob);
    }

    @Transactional
    public Snapshot createSnapshot(SnapshotCreateRequest request) {
        if(request.files() == null || request.files().isEmpty()) {
            throw new IllegalArgumentException("Snapshot must contain at least one file");
        }

        // Збираємо унікальні blobId із запиту
        Set<UUID> requestBlobUUIds = request.files().stream()
                .map(SnapshotFileRequest::blobId)
                .map(UUID::fromString)
                .collect(Collectors.toSet());

        List<BlobMetadata> foundBlobs = blobRepository.findAllById(requestBlobUUIds);

        if(foundBlobs.size() != requestBlobUUIds.size()) {
            Set<String> foundIdsAsString = foundBlobs.stream()
                    .map(b -> b.getId().toString())
                    .collect(Collectors.toSet());
            Set<String> missingIds = request.files().stream()
                    .map(SnapshotFileRequest::blobId)
                    .filter(id -> !foundIdsAsString.contains(id))
                    .collect(Collectors.toSet());
            throw new IllegalArgumentException("The following blob IDs do not exist" + missingIds);
        }

        // Create
        Snapshot snapshot = new Snapshot();
        snapshot.setName(request.name());
        snapshot.setDescription(request.description());
        snapshot.setCreatedAt(Instant.now());

        long totalSize = request.files().stream()
                .mapToLong(SnapshotFileRequest::size)
                .sum();

        snapshot.setTotalSize(totalSize);
        snapshot.setFileCount(request.files().size());

        snapshot = snapshotRepository.save(snapshot);

        // 5. Создаём BackupFile записи + увеличиваем refcount
        // Для удобства делаем map UUID → BlobMetadata один раз
        Map<UUID, BlobMetadata> blobMap = foundBlobs.stream()
                .collect(Collectors.toMap(BlobMetadata::getId, b -> b));

        for (SnapshotFileRequest fileReq : request.files()) {

            String path = fileReq.path();
            if (path == null || path.trim().isEmpty()) {
                throw new IllegalArgumentException("Path cannot be empty");
            }
            if (path.contains("..") || path.startsWith("/")) {  // defense from traversal
                throw new IllegalArgumentException("Invalid path: " + path);
            }
            if (path.length() > 1024) {
                throw new IllegalArgumentException("Path too long: " + path);
            }

            UUID blobUuid = UUID.fromString(fileReq.blobId());

            BackupFile file = new BackupFile();
            file.setSnapshot(snapshot);
            file.setPath(path);
            file.setBlobId(blobUuid);
            file.setSize(fileReq.size());
            file.setModifiedAt(fileReq.modifiedAt());

            backupFileRepository.save(file);

            // увеличиваем refcount
            BlobMetadata meta = blobMap.get(blobUuid);
            if (meta == null) {
                throw new IllegalStateException("BlobMetadata disappeared after check");
            }

            incrementRefCount(meta);
            blobRepository.save(meta);
        }

        return snapshot;
    }

    public Snapshot getSnapshot(Long id) {
        return snapshotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Snapshot not found with id:" + id));
    }

    public List<Snapshot> listSnapshots() {
        return snapshotRepository.findAllByOrderByCreatedAtDesc();
    }

}

package org.example.e2eencryptedmediaserv.server.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.example.e2eencryptedmediaserv.server.model.BackupFile;
import org.example.e2eencryptedmediaserv.server.model.BlobMetadata;
import org.example.e2eencryptedmediaserv.server.model.Snapshot;
import org.example.e2eencryptedmediaserv.server.model.dto.BlobResponse;
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
    public BlobMetadata handleUpload(InputStream stream, BlobUploadMetadata meta, Long userId) {
        Optional<BlobMetadata> existing = blobRepository.findByUserIdAndHash(userId, meta.cipherHash());

        if(existing.isPresent()) {
            BlobMetadata blob = existing.get();
            incrementRefCount(blob);
            return blobRepository.save(blob);
        }

        UUID id = UUID.randomUUID();

        String storageKey = storage.put(id, stream, userId);

        BlobMetadata blob = new BlobMetadata();
        blob.setId(id);
        blob.setHash(meta.cipherHash());
        blob.setStorageKey(storageKey);
        blob.setSize(meta.size());
        blob.setRefcount(1);
        blob.setCreatedAt(Instant.now());
        blob.setMetadata(meta);
        blob.setUserId(userId);

        return blobRepository.save(blob);
    }

    @Transactional
    public Snapshot createSnapshot(SnapshotCreateRequest request, Long userId) {
        if (request.files() == null || request.files().isEmpty()) {
            throw new IllegalArgumentException("Snapshot must contain at least one file");
        }

        // unique blobId
        Set<UUID> requestBlobIds = request.files().stream()
                .map(f -> UUID.fromString(f.blobId()))
                .collect(Collectors.toSet());

        // download blobs
        List<BlobMetadata> foundBlobs = blobRepository.findAllById(requestBlobIds);

        if (foundBlobs.size() != requestBlobIds.size()) {
            throw new IllegalArgumentException("Some blob IDs do not exist");
        }

        // check owner
        Map<UUID, BlobMetadata> blobMap = foundBlobs.stream()
                .collect(Collectors.toMap(BlobMetadata::getId, b -> b));

        for (SnapshotFileRequest fileReq : request.files()) {
            UUID blobUuid = UUID.fromString(fileReq.blobId());
            BlobMetadata realBlob = blobMap.get(blobUuid);

            if (!realBlob.getUserId().equals(userId)) {
                throw new SecurityException("Blob " + blobUuid + " belongs to another user");
            }

            if (fileReq.size() == null || !fileReq.size().equals(realBlob.getSize())) {
                throw new IllegalArgumentException(
                        "Size mismatch for blob " + blobUuid +
                                ": expected " + realBlob.getSize() + ", got " + fileReq.size()
                );
            }

            String path = fileReq.path();
            if (path == null || path.trim().isEmpty()) {
                throw new IllegalArgumentException("Path cannot be empty");
            }

            // traversal defense
            path = path.replaceAll("^/+", "");           // remove front slashes
            path = path.replaceAll("\\.+", ".");         // remove multiple dots
            if (path.contains("..") || path.startsWith("/") || path.startsWith("\\")) {
                throw new IllegalArgumentException("Invalid path: traversal detected - " + path);
            }
            if (path.length() > 512) {
                throw new IllegalArgumentException("Path too long");
            }
        }

        Snapshot snapshot = new Snapshot();
        snapshot.setName(request.name());
        snapshot.setDescription(request.description());
        snapshot.setCreatedAt(Instant.now());
        snapshot.setUserId(userId);

        long totalSize = request.files().stream()
                .mapToLong(SnapshotFileRequest::size)
                .sum();

        snapshot.setTotalSize(totalSize);
        snapshot.setFileCount(request.files().size());

        snapshot = snapshotRepository.save(snapshot);

        // refcount update + snapshot
        Map<UUID, Long> blobUsageCount = new HashMap<>();

        for (SnapshotFileRequest fileReq : request.files()) {
            UUID blobUuid = UUID.fromString(fileReq.blobId());
            String path = fileReq.path().replaceAll("^/+", "");

            BackupFile file = new BackupFile();
            file.setSnapshot(snapshot);
            file.setPath(path);
            file.setBlobId(blobUuid);
            file.setSize(fileReq.size());
            file.setModifiedAt(fileReq.modifiedAt());
            file.setUserId(userId);

            backupFileRepository.save(file);

            blobUsageCount.merge(blobUuid, 1L, Long::sum);
        }

        blobUsageCount.forEach((blobId, count) -> {
            BlobMetadata meta = blobMap.get(blobId);
            meta.setRefcount(meta.getRefcount() + count.intValue());
            blobRepository.save(meta);
        });

        return snapshot;
    }

    @Transactional
    public void deleteSnapshot(Long snapshotId, Long userId) {
        Snapshot snapshot = snapshotRepository.findByIdAndUserId(snapshotId, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Snapshot not found or access denied: " + snapshotId));

        List<BackupFile> files = backupFileRepository.findBySnapshotId(snapshotId);

        for (BackupFile file : files) {
            BlobMetadata blob = blobRepository.findById(file.getBlobId())
                    .orElse(null);

            if (blob != null) {
                blob.setRefcount(blob.getRefcount() - 1);
                if (blob.getRefcount() <= 0) {
                    storage.delete(blob.getStorageKey());
                    blobRepository.delete(blob);
                } else {
                    blobRepository.save(blob);
                }
            }
        }

        backupFileRepository.deleteAll(files);
        snapshotRepository.delete(snapshot);
    }

    public BlobWithData getBlobWithData(UUID id, Long userId) {
        BlobMetadata blob = blobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Blob not found: " + id));

        if (!blob.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to blob: " + id);
        }

        try {
            byte[] data = storage.get(blob.getStorageKey()).readAllBytes();
            return new BlobWithData(blob, data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read blob data: " + id, e);
        }
    }

    public List<BlobResponse> getUserBlobs(Long userId) {
        List<BlobMetadata> blobs = blobRepository.findByUserId(userId);

        return blobs.stream()
                .map(this::mapToBlobResponse)
                .toList();
    }

    public Snapshot getSnapshot(Long id, Long userId) {
        return snapshotRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Snapshot with id: " + id + " not found or access denied:"));
    }

    public List<BackupFile> getSnapshotFiles(Long snapshotId, Long userId) {
        getSnapshot(snapshotId, userId);
        return backupFileRepository.findBySnapshotId(snapshotId);
    }

    public List<Snapshot> listSnapshots(Long userId) {
        return snapshotRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    private BlobResponse mapToBlobResponse(BlobMetadata blob) {
        return new BlobResponse(
                blob.getId(),
                blob.getMetadata().originalFilename(),
                blob.getSize(),
                blob.getCreatedAt(),
                blob.getStorageKey(),
                blob.getRefcount() > 1
        );
    }

    public record BlobWithData(BlobMetadata metadata, byte[] data) {}
}


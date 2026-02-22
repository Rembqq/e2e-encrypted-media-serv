package org.example.e2eencryptedmediaserv.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.example.e2eencryptedmediaserv.server.model.BlobMetadata;
import org.example.e2eencryptedmediaserv.server.model.BlobUploadMetadata;
import org.example.e2eencryptedmediaserv.server.repository.BlobMetadataRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class BlobService {
    private final BlobMetadataRepository repository;
    private final BlobStorage storage;

    public BlobService(BlobMetadataRepository repository, BlobStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Transactional
    public BlobMetadata handleUpload(InputStream stream, BlobUploadMetadata meta) throws IOException {
        Optional<BlobMetadata> existing = repository.findByHash(meta.cipherHash());

        if(existing.isPresent()) {
            BlobMetadata blob = existing.get();
            blob.setRefcount(blob.getRefcount() + 1);
            return repository.save(blob);
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

        return repository.save(blob);
    }
}

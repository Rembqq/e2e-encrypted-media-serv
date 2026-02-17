package org.example.e2eencryptedmediaserv.server.service;

import jakarta.transaction.Transactional;
import org.example.e2eencryptedmediaserv.server.model.BlobMetadata;
import org.example.e2eencryptedmediaserv.server.repository.BlobMetadataRepository;
import org.springframework.stereotype.Service;

@Service
public class BlobService {
    private final BlobMetadataRepository repository;
    private final BlobStorage storage;

    public BlobService(BlobMetadataRepository repository, BlobStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Transactional
    public BlobMetadata handle

}

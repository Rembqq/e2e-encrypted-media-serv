package org.example.e2eencryptedmediaserv.server.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public interface BlobStorage {
    String put(UUID blobId, InputStream stream) throws Exception;
    InputStream get(String storageKey) throws Exception;
    void delete(String storageKey) throws Exception;

}

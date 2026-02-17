package org.example.e2eencryptedmediaserv.server.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FilesystemBlobStorage implements BlobStorage {

    private final Path root = Paths.get("blob-storage");

    public FilesystemBlobStorage() throws IOException {
        Files.createDirectories(root);
    }

    @Override
    public String put(UUID blobId, InputStream stream) throws IOException {
        String filename = blobId.toString();
        Path target = root.resolve(filename);
        Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }

    @Override
    public InputStream get(String storageKey) throws IOException {
        return Files.newInputStream(root.resolve(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(root.resolve(storageKey));
    }
}

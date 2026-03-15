package org.example.e2eencryptedmediaserv.server.service.impl;

import org.example.e2eencryptedmediaserv.server.service.BlobStorage;
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
    public String put(UUID blobId, InputStream stream, Long userId) {
        String filename = blobId.toString();
        Path target = root.resolve(filename);
        try {
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("FilesystemddStorage put method failed");
        }

        return filename;
    }

    @Override
    public InputStream get(String storageKey) {
        try {
            return Files.newInputStream(root.resolve(storageKey));
        } catch (IOException e) {
            throw new RuntimeException("MinioBlobStorage put method failed");
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(root.resolve(storageKey));;
        } catch (IOException e) {
            throw new RuntimeException("MinioBlobStorage put method failed");
        }
    }
}

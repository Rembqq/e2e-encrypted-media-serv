package org.example.e2eencryptedmediaserv.server.service.impl;

import io.minio.*;
import jakarta.annotation.PostConstruct;
import org.example.e2eencryptedmediaserv.server.service.BlobStorage;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.util.UUID;

@Primary
@Service
public class MinioBlobStorage implements BlobStorage {

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioBlobStorage(
            @Value("${minio.url}")         String url,
            @Value("${minio.access-key}")  String accessKey,
            @Value("${minio.secret-key}")  String secretKey,
            @Value("${minio.bucket}")      String bucketName,
            @Value("${minio.region:}")     String region
    ) {
        this.bucketName = bucketName;

        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey);

        if (region != null && !region.isBlank()) {
            builder.region(region);
        }

        this.minioClient = builder.build();
    }

    @PostConstruct
    public void init() throws Exception {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("MinIO init failed. URL=" + minioClient +
                    ", bucket=" + bucketName +
                    ", cause=" + e.getClass().getName() +
                    ": " + e.getMessage(), e);
        }
    }

    @Override
    public String put(UUID blobId, InputStream stream, Long userId) {
        String key = "user-" + userId + "/blobs/" + blobId.toString();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .stream(stream, -1, 10485760)
                            .contentType("application/octet-stream")
                            .build()
            );
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload blob to MinIO: " + blobId + ", bucket: " + bucketName, e);
        }
    }

    @Override
    public InputStream get(String storageKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to get the blob");
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete the blob");
        }
    }
}
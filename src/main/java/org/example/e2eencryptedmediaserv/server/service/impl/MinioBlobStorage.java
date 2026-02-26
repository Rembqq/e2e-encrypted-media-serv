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
            @Value("${minio.url}")          String url,
            @Value("${minio.access-key}")          String accessKey,
            @Value("${minio.secret-key}")          String secretKey,
            @Value("${minio.bucket}")                   String bucketName
    ) {
        this.bucketName = bucketName;

        this.minioClient = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    @PostConstruct
    public void init() throws Exception{
        if(!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    @Override
    public String put(UUID blobId, InputStream stream) throws Exception {
        String key = blobId.toString();

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(key)
                        .stream(stream, -1, 10485760)
                        .contentType("application/octet-stream")
                        .build()
        );

        return key;
    }

    @Override
    public InputStream get(String storageKey) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(storageKey)
                        .build()
        );
    }

    @Override
    public void delete(String storageKey) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(storageKey)
                        .build()
        );
    }
}

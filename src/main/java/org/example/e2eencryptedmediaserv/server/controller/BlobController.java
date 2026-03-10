package org.example.e2eencryptedmediaserv.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.e2eencryptedmediaserv.server.model.BlobMetadata;
import org.example.e2eencryptedmediaserv.server.model.dto.BlobUploadMetadata;
import org.example.e2eencryptedmediaserv.server.service.BlobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/blobs")
public class BlobController {

    private final BlobService service;
    private final ObjectMapper objectMapper;

    public BlobController(BlobService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestPart("blob") MultipartFile file,
            @RequestPart("metadata") String metadataJson
            ) throws Exception {
        System.out.println("POST /blobs вызван. Пользователь: " +
                SecurityContextHolder.getContext().getAuthentication().getName());
        System.out.println("Роли: " +
                SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        BlobUploadMetadata metadata =
                objectMapper.readValue(metadataJson, BlobUploadMetadata.class);

        BlobMetadata saved =
                service.handleUpload(file.getInputStream(), metadata);

        Map<String, Object> response = Map.of(
                "blobId", saved.getId(),
                "storageKey", saved.getStorageKey(),
                "deduped", saved.getRefcount() > 1
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }
}

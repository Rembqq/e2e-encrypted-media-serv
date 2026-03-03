package org.example.e2eencryptedmediaserv.server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.e2eencryptedmediaserv.server.model.Snapshot;
import org.example.e2eencryptedmediaserv.server.model.dto.SnapshotCreateRequest;
import org.example.e2eencryptedmediaserv.server.model.dto.SnapshotResponse;
import org.example.e2eencryptedmediaserv.server.service.BlobService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/snapshots")
@RequiredArgsConstructor
public class SnapshotController {
    private final BlobService blobService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SnapshotResponse create(@Valid @RequestBody SnapshotCreateRequest request) {
        Snapshot snapshot = blobService.createSnapshot(request);
        return mapToResponse(snapshot);
    }

    @GetMapping
    public List<SnapshotResponse> list() {
        return blobService.listSnapshots().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public SnapshotResponse getOne(@PathVariable Long id) {
        Snapshot snapshot = blobService.getSnapshot(id);
        return mapToResponse(snapshot);
    }

    private SnapshotResponse mapToResponse(Snapshot s) {
        return new SnapshotResponse(
                s.getId(),
                s.getName(),
                s.getDescription(),
                s.getCreatedAt(),
                s.getTotalSize(),
                s.getFileCount()
        );
    }

}

package org.example.e2eencryptedmediaserv.server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.e2eencryptedmediaserv.server.model.BackupFile;
import org.example.e2eencryptedmediaserv.server.model.Snapshot;
import org.example.e2eencryptedmediaserv.server.model.User;
import org.example.e2eencryptedmediaserv.server.model.dto.SnapshotCreateRequest;
import org.example.e2eencryptedmediaserv.server.model.dto.SnapshotFileRequest;
import org.example.e2eencryptedmediaserv.server.model.dto.SnapshotResponse;
import org.example.e2eencryptedmediaserv.server.security.CustomUserDetails;
import org.example.e2eencryptedmediaserv.server.service.BlobService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.channels.AcceptPendingException;
import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/snapshots")
@RequiredArgsConstructor
public class SnapshotController {
    private final BlobService blobService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SnapshotResponse create(@Valid @RequestBody SnapshotCreateRequest request,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromPrincipal(userDetails);
        Snapshot snapshot = blobService.createSnapshot(request, userId);
        return mapToResponse(snapshot);
    }

    @GetMapping
    public List<SnapshotResponse> list(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = getUserIdFromPrincipal(userDetails);
        return blobService.listSnapshots(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        blobService.deleteSnapshot(id, getUserIdFromPrincipal(userDetails));
    }

    @GetMapping("/{id}")
    public SnapshotResponse getOne(@PathVariable Long id,
                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = getUserIdFromPrincipal(userDetails);
        Snapshot snapshot = blobService.getSnapshot(id, userId);
        List<BackupFile> files = blobService.getSnapshotFiles(id, userId);

        List<SnapshotFileRequest> fileRequests = files.stream()
                .map(f -> new SnapshotFileRequest(
                        f.getPath(),
                        f.getBlobId().toString(),
                        f.getSize(),
                        f.getModifiedAt()
                ))
                .toList();

        return new SnapshotResponse(
                snapshot.getId(),
                snapshot.getName(),
                snapshot.getDescription(),
                snapshot.getCreatedAt(),
                snapshot.getTotalSize(),
                snapshot.getFileCount(),
                fileRequests
        );
    }

    private Long getUserIdFromPrincipal(UserDetails userDetails) {
        if(userDetails == null) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        if (userDetails instanceof CustomUserDetails user) {
            return user.getId();
        }
        throw new IllegalStateException("Unknown UserDetails implementation");
    }

    private SnapshotResponse mapToResponse(Snapshot s) {
        return new SnapshotResponse(
                s.getId(),
                s.getName(),
                s.getDescription(),
                s.getCreatedAt(),
                s.getTotalSize(),
                s.getFileCount(),
                null
        );
    }

}

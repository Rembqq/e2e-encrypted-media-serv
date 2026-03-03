//package org.example.e2eencryptedmediaserv.server.service;
//
//import org.example.e2eencryptedmediaserv.server.model.BlobMetadata;
//import org.example.e2eencryptedmediaserv.server.model.dto.BlobUploadMetadata;
//import org.example.e2eencryptedmediaserv.server.repository.BlobMetadataRepository;
//import org.junit.jupiter.api.Test;
//
//import java.io.ByteArrayInputStream;
//import java.io.InputStream;
//import java.time.Instant;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//public class BlobServiceTest {
//
//    private final BlobMetadataRepository repo = mock(BlobMetadataRepository.class);
//    private final BlobStorage storage = mock(BlobStorage.class);
//    private final BlobService service = new BlobService(repo, storage);
//
//    @Test
//    void shouldCreateNewBlobWhenNoDuplicate() throws Exception {
//        // given
//        String hash = "abc123hash-" + UUID.randomUUID();  // уникальный каждый раз
//        BlobUploadMetadata meta = new BlobUploadMetadata(
//                "client-1", "photo.jpg", 1024L, Instant.now(), hash);
//        InputStream stream = new ByteArrayInputStream(new byte[1024]);
//
//        when(storage.put(any(), any())).thenReturn("storage/key/xyz");
//
//        when(repo.findByHash(hash)).thenReturn(Optional.empty());
//
//        // when
//        BlobMetadata saved = service.handleUpload(stream, meta);
//
//        // then
//        assertThat(saved).isNotNull();
//        assertThat(saved.getId()).isNotNull();
//        assertThat(saved.getHash()).isEqualTo(hash);
//        assertThat(saved.getRefcount()).isEqualTo(1);
//        assertThat(saved.getCreatedAt()).isNotNull();
//        assertThat(saved.getMetadata().originalFilename()).isEqualTo("photo.jpg");
//
//        // проверка в "БД"
//        BlobMetadata fromDb = repo.findById(saved.getId()).orElseThrow();
//        assertThat(fromDb.getRefcount()).isEqualTo(1);
//    }
//
//    @Test
//    void shouldIncrementRefcountOnDuplicate() throws Exception {
//        String hash = "dup-hash";
//        BlobUploadMetadata meta = new BlobUploadMetadata("client", "file.jpg", 1024L, Instant.now(), hash);
//        InputStream stream = mock(InputStream.class);
//
//        BlobMetadata existing = new BlobMetadata();
//        existing.setId(UUID.randomUUID());
//        existing.setRefcount(3);
//        when(repo.findByHash(hash)).thenReturn(Optional.of(existing));
//
//        BlobMetadata result = service.handleUpload(stream, meta);
//
//        assertThat(result).isSameAs(existing);
//        assertThat(result.getRefcount()).isEqualTo(4);
//        verify(storage, never()).put(any(), any());
//        verify(repo).save(existing);
//    }
//
//    @Test
//    void incrementRefCount_handlesNullSafely() {
//        BlobMetadata meta = new BlobMetadata(); // refcount == null
//
//        service.incrementRefCount(meta);
//        assertThat(meta.getRefcount()).isEqualTo(1);
//
//        service.incrementRefCount(meta);
//        assertThat(meta.getRefcount()).isEqualTo(2);
//    }
//
//    @Test
//    void shouldThrowWhenStorageFails() throws Exception {
//        BlobUploadMetadata meta = new BlobUploadMetadata("client", "fail", 100L, Instant.now(), "fail-hash");
//        InputStream stream = mock(InputStream.class);
//
//        when(storage.put(any(), any())).thenThrow(new RuntimeException("storage down"));
//        when(repo.findByHash(any())).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> service.handleUpload(stream, meta))
//                .isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("down");
//
//        verify(repo, never()).save(any());
//    }
//
//}

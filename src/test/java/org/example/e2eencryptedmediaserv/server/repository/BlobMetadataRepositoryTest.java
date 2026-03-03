package org.example.e2eencryptedmediaserv.server.repository;

import org.example.e2eencryptedmediaserv.server.model.BlobMetadata;
import org.example.e2eencryptedmediaserv.server.model.dto.BlobUploadMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
public class BlobMetadataRepositoryTest {

    @Autowired
    BlobMetadataRepository repo;

    @Test
    void findByHash_found() {
        String hash = "hash-test-abc" + UUID.randomUUID();
        BlobMetadata entity = createTestBlob(hash);

        Optional<BlobMetadata> found = repo.findByHash(hash);
        assertThat(found).isPresent();
        assertThat(found.get().getMetadata().originalFilename()).isEqualTo("test.jpg");
    }

    @Test
    void findByHash_notFound() {
        assertThat(repo.findByHash("no-such-hash")).isEmpty();
    }

    @Test
    void jsonb_metadata_persisted() {
        BlobMetadata e = createTestBlob("json-hash");
        repo.saveAndFlush(e);

        BlobMetadata loaded = repo.findById(e.getId()).orElseThrow();
        assertThat(loaded.getMetadata().clientId()).isEqualTo("clientX");
    }

    private BlobMetadata createTestBlob(String hash) {
        BlobMetadata m = new BlobMetadata();
        m.setId(UUID.randomUUID());
        m.setHash(hash);
        m.setStorageKey("test/storage/" + UUID.randomUUID());
        m.setSize(8192L);
        m.setRefcount(1);
        m.setCreatedAt(Instant.now());
        m.setMetadata(new BlobUploadMetadata("test-client-007",
                "example.png", 8192L, Instant.now(), hash));
        return m;
    }

}

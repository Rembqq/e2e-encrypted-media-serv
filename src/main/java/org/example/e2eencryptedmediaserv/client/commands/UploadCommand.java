package org.example.e2eencryptedmediaserv.client.commands;


import okhttp3.*;
import org.example.e2eencryptedmediaserv.client.crypto.EncryptionService;
import org.example.e2eencryptedmediaserv.client.model.UploadMetadata;
import org.example.e2eencryptedmediaserv.client.utils.CliUtils;
import picocli.CommandLine;
import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "upload",
        description = "Encrypt file on client side and upload to server"
)
public class UploadCommand implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "Path to the file to download")
    private File file;

    @CommandLine.Option(names = {"--server", "-s"}, required = true, description = "Server URL (e.g. http://localhost:8080)")
    private String serverUrl;

    @CommandLine.Option(names = {"--key", "-k"}, required = true, description = "Base64-encoded 256-bit encryption key")
    private String keyBase64;

    @CommandLine.Option(names = {"--token", "-t"}, defaultValue = "dummy-token", description = "Bearer authorization token (if required)")
    private String token;

    @CommandLine.Option(names = {"--client-id", "-c"}, defaultValue = "alice", description = "Client ID")
    private String clientId;

    @Override
    public Integer call() throws Exception {
        if (!file.exists() || !file.isFile()) {
            System.err.println("Error: File not found or not a file: " + file.getAbsolutePath());
            return 1;
        }

        // 1. read from file
        byte[] plaintext = Files.readAllBytes(file.toPath());
        long originalSize = plaintext.length;
        String originalFilename = file.getName();
        Instant modifiedAt = Files.getLastModifiedTime(file.toPath()).toInstant();

        // 2. prepare key
        // 3. AES-GCM encryption
        // 4. Calculate SHA-256 from the entire blob => cipherHash
        EncryptionService crypto = new EncryptionService(keyBase64);
        var encData = crypto.encryptForUpload(plaintext);

        // 5. Metadata
        UploadMetadata metadata = new UploadMetadata(
                clientId,
                originalFilename,
                originalSize,
                modifiedAt,
                encData.plaintextHash()
        );

        String blobId = CliUtils.uploadBlob(
                serverUrl, token, encData.blob(), originalFilename, metadata
        );

        if (blobId != null) {
            System.out.printf("Success -> Blob ID: %s%n", blobId);
            System.out.printf("File: %s | Plain hash: %s | Size: %,d bytes%n",
                    originalFilename, encData.plaintextHash(), encData.blob().length);
            return 0;
        }
        return 1;
    }
}

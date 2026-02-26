package org.example.e2eencryptedmediaserv.client.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.*;
import org.example.e2eencryptedmediaserv.client.model.UploadMetadata;
import picocli.CommandLine;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
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

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .build();

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public Integer call() throws Exception {
        if (!file.exists() || !file.isFile()) {
            System.err.println("Error: File not found or not a file: " + file.getAbsolutePath());
            return 1;
        }

        // 1. prepare key
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(keyBase64);
            if (keyBytes.length != 32) {
                System.err.println("Error: The key must be exactly 32 bytes (256 bits)");
                return 1;
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error: Invalid base64 key");
            return 1;
        }

        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

        // 2. read from file
        byte[] plaintext = Files.readAllBytes(file.toPath());
        long originalSize = plaintext.length;
        String originalFilename = file.getName();
        Instant modifiedAt = Files.getLastModifiedTime(file.toPath()).toInstant();

        // 3. AES-GCM encryption
        SecureRandom random = new SecureRandom();
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        byte[] ciphertext = cipher.doFinal(plaintext);

        // Blob = nonce + ciphertext (standard storage method)
        byte[] blob = new byte[nonce.length + ciphertext.length];
        System.arraycopy(nonce, 0, blob, 0, nonce.length);
        System.arraycopy(ciphertext, 0, blob, nonce.length, ciphertext.length);

        // 4. Calculate SHA-256 from the entire blob => cipherHash
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = sha256.digest(plaintext);
        String contentHash = bytesToHex(hashBytes);

        // 5. Metadata
        UploadMetadata metadata = new UploadMetadata(
                clientId,
                originalFilename,
                originalSize,
                modifiedAt,
                contentHash
        );

        String metadataJson;
        try {
            metadataJson = mapper.writeValueAsString(metadata);
        } catch (Exception e) {
            System.err.println("Metadata serialization error: " + e.getMessage());
            return 1;
        }

        // 6. Multipart-request
        RequestBody fileBody = RequestBody.create(blob, MediaType.get("application/octet-stream"));
        RequestBody metadataBody = RequestBody.create(metadataJson, MediaType.get("application/json"));

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("blob", originalFilename, fileBody)
                .addFormDataPart("metadata", "metadata.json", metadataBody)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl + "/api/v1/blobs")
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                System.out.println("Successfully uploaded!");
                System.out.println("  File/Blob:          " + originalFilename);
                System.out.println("  cipherHash:    " + contentHash);
                System.out.println("  Blob size:   " + blob.length + " bytes");
                System.out.println("  Original size:  " + originalSize + " bytes");
                System.out.println("  Server Response: " + response.body().string());
                return 0;
            } else {
                String errorBody = response.body() != null ? response.body().string() : "";
                System.err.println("Loading error: " + response.code() + " " + response.message());
                System.err.println("Server response: " + errorBody);
                return 1;
            }
        } catch (Exception e) {
            System.err.println("Connection error or executing request: " + e.getMessage());
            return 1;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

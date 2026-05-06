package org.example.e2eencryptedmediaserv.client.crypto;

import org.bouncycastle.util.encoders.Hex;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Base64;
import java.util.HexFormat;

public class EncryptionService {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH = 128;  // 16 bytes
    private static final int NONCE_LENGTH = 12;

    private final SecretKey key;

    public EncryptionService(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("Key must be exactly 32 bytes (256-bit)");
        }

        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public UploadEncryptionResult encryptForUpload(byte[] plaintext) throws Exception {
        byte[] blob = encryptInternal(plaintext);
        String plaintextHash = sha256Hex(plaintext);
        return new UploadEncryptionResult(blob, plaintextHash);
    }

    public BackupEncryptionResult encryptForBackup(byte[] plaintext) throws Exception {
        byte[] blob = encryptInternal(plaintext);
        String blobHash = sha256Hex(blob);
        return new BackupEncryptionResult(blob, blobHash);
    }

    private byte[] encryptInternal(byte[] plaintext) throws Exception {
        byte[] nonce = new byte[NONCE_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(nonce);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] ciphertext = cipher.doFinal();

        byte[] blob = new byte[nonce.length + ciphertext.length];
        System.arraycopy(nonce, 0, blob, 0, nonce.length);
        System.arraycopy(ciphertext, 0, blob, nonce.length, ciphertext.length);

        return blob;
    }

    public byte[] decrypt(byte[] encryptedBlob) throws Exception {
        if (encryptedBlob.length < NONCE_LENGTH + TAG_LENGTH / 8) { // 12 nonce + 16 tag + minimum 0 bytes of data
            throw new IllegalArgumentException("Encrypted data too short");
        }

        byte[] nonce = new byte[NONCE_LENGTH];
        System.arraycopy(encryptedBlob, 0, nonce, 0, NONCE_LENGTH);

        byte[] ciphertext = new byte[encryptedBlob.length - NONCE_LENGTH];
        System.arraycopy(encryptedBlob, NONCE_LENGTH, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, nonce);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher.doFinal(ciphertext);
    }

    private String sha256Hex(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] digest = sha.digest(data);
        return HexFormat.of().formatHex(digest);
    }

    // Records for results

    public record UploadEncryptionResult(byte[] blob, String plaintextHash) {}

    public record BackupEncryptionResult(byte[] blob, String blobHash) {}
}

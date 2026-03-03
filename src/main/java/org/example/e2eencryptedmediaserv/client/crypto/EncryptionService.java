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
    private static final int TAG_LENGTH = 128;  // 16 байт
    private static final int NONCE_LENGTH = 12;

    private final SecretKey key;

    public EncryptionService(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public EncryptionResult encrypt(byte[] plaintext) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] nonce = new byte[NONCE_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(nonce);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] ciphertext = cipher.doFinal();

        byte[] full = new byte[nonce.length + ciphertext.length];
        System.arraycopy(nonce, 0, full, 0, nonce.length);
        System.arraycopy(ciphertext, 0, full, nonce.length, ciphertext.length);

        String hash = computeSha256Hex(full);
        return new EncryptionResult(full, hash);
    }

    // public byte[] decrypt(byte[] encryptedData) throws Exception {  }

    private String computeSha256Hex(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] digest = sha.digest(data);
        return HexFormat.of().formatHex(digest);
    }

    public record EncryptionResult(byte[] data, String hash) {}
}

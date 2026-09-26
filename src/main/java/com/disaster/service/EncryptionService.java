package com.disaster.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption service for sensitive donor data at rest.
 * <p>
 * - Random 12-byte IV per encryption; 128-bit auth tag.
 * - Ciphertext stored with an {@code enc:v1:} prefix so legacy plaintext values
 *   remain readable and re-encrypting is idempotent.
 * - Key comes from the {@code rrq.encryption-key} property (any-length secret,
 *   deterministically derived to a 32-byte AES key). Rotate by changing the property
 *   and re-saving records.
 */
@Service
public class EncryptionService {

    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public EncryptionService(@Value("${rrq.encryption-key:}") String base64Key) {
        try {
            byte[] raw;
            if (base64Key == null || base64Key.isBlank()) {
                raw = new byte[32];
                random.nextBytes(raw);
                System.getLogger(EncryptionService.class.getName())
                        .log(System.Logger.Level.WARNING,
                                "rrq.encryption-key not set - using an EPHEMERAL random key. "
                                        + "Encrypted records will be unreadable after restart. Set the property in production.");
            } else {
                raw = Base64.getDecoder().decode(base64Key.trim());
            }
            // Normalize any key length to exactly 32 bytes for AES-256
            if (raw.length != 32) {
                raw = MessageDigest.getInstance("SHA-256").digest(raw);
            }
            this.key = new SecretKeySpec(raw, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize EncryptionService", e);
        }
    }

    /** Encrypts plaintext; null/blank and already-encrypted values pass through unchanged. */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank() || plaintext.startsWith(PREFIX)) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return PREFIX
                    + Base64.getEncoder().encodeToString(iv)
                    + ":"
                    + Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /** Decrypts values produced by {@link #encrypt(String)}; anything else is returned as-is. */
    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            return stored;
        }
        try {
            String[] parts = stored.substring(PREFIX.length()).split(":", 2);
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed (wrong key or corrupted data?)", e);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }
}

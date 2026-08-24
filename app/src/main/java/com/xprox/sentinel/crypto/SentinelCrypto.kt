package com.xprox.sentinel.crypto

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SentinelCrypto {
    private const val AES_GCM_ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PIN_SALT = "SentinelHotspotSecureSalt_v1"

    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically secure 256-bit (32 bytes) master key.
     */
    fun generate256BitKey(): ByteArray {
        val key = ByteArray(32)
        secureRandom.nextBytes(key)
        return key
    }

    /**
     * Generates a 256-bit key formatted as Base64 string.
     */
    fun generateKeyBase64(): String {
        return Base64.encodeToString(generate256BitKey(), Base64.NO_WRAP)
    }

    /**
     * Generates a cryptographically secure device token (Base64).
     */
    fun generateDeviceToken(): String {
        val tokenBytes = ByteArray(32)
        secureRandom.nextBytes(tokenBytes)
        return Base64.encodeToString(tokenBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /**
     * Generates a random nonce string for request signatures.
     */
    fun generateNonce(): String {
        val nonceBytes = ByteArray(16)
        secureRandom.nextBytes(nonceBytes)
        return Base64.encodeToString(nonceBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /**
     * Derives a 256-bit AES key from a PIN code and predefined salt using SHA-256.
     */
    fun deriveKeyFromPin(pin: String): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        val combined = "$PIN_SALT:$pin".toByteArray(StandardCharsets.UTF_8)
        return md.digest(combined)
    }

    /**
     * Encrypts plaintext string using AES-256-GCM.
     * Output format: Base64([12-byte IV] + [Ciphertext + 16-byte GCM Tag])
     */
    fun encrypt(plaintext: String, key: ByteArray): String {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val ciphertextWithTag = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))

        val combined = ByteArray(iv.size + ciphertextWithTag.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertextWithTag, 0, combined, iv.size, ciphertextWithTag.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypts Base64 payload containing [12-byte IV] + [Ciphertext + 16-byte GCM Tag] using AES-256-GCM.
     */
    fun decrypt(base64Payload: String, key: ByteArray): String {
        val combined = Base64.decode(base64Payload.trim(), Base64.NO_WRAP)
        if (combined.size < GCM_IV_LENGTH + 16) {
            throw IllegalArgumentException("Invalid ciphertext payload: length too short")
        }

        val iv = ByteArray(GCM_IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)

        val ciphertextWithTagLength = combined.size - GCM_IV_LENGTH
        val ciphertextWithTag = ByteArray(ciphertextWithTagLength)
        System.arraycopy(combined, GCM_IV_LENGTH, ciphertextWithTag, 0, ciphertextWithTagLength)

        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        val decryptedBytes = cipher.doFinal(ciphertextWithTag)

        return String(decryptedBytes, StandardCharsets.UTF_8)
    }
}

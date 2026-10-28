package com.example.data.backup

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides authenticated encryption and decryption for TaskNest backup payloads using
 * `AES/GCM/NoPadding` (12-byte random IV, 128-bit authentication tag) with key derivation via
 * `PBKDF2WithHmacSHA256` (65,536 iterations, 16-byte random salt, 256-bit key).
 *
 * Wire format (before Base64 encoding with header `TNENC1:`):
 * - 16 bytes: Salt
 * - 12 bytes: GCM IV
 * - N bytes: Ciphertext + 16-byte GCM authentication tag
 */
@Singleton
class BackupCryptoManager @Inject constructor() {

  companion object {
    const val ENCRYPTED_HEADER_PREFIX = "TNENC1:"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val ITERATION_COUNT = 65_536
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
  }

  private val secureRandom = SecureRandom()

  fun isEncryptedPayload(payload: String): Boolean {
    return payload.trimStart().startsWith(ENCRYPTED_HEADER_PREFIX)
  }

  fun encrypt(plainText: String, passphrase: String): String {
    require(passphrase.isNotEmpty()) { "Passphrase must not be empty" }

    val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
    val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
    val secretKey = deriveAesKey(passphrase, salt)

    val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
    val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

    val combined = ByteBuffer.allocate(SALT_LENGTH_BYTES + IV_LENGTH_BYTES + cipherBytes.size)
      .put(salt)
      .put(iv)
      .put(cipherBytes)
      .array()

    val encoded = Base64.getEncoder().encodeToString(combined)
    return "$ENCRYPTED_HEADER_PREFIX$encoded"
  }

  fun decrypt(encryptedPayload: String, passphrase: String): String {
    require(passphrase.isNotEmpty()) { "Passphrase must not be empty" }
    val trimmed = encryptedPayload.trim()
    require(trimmed.startsWith(ENCRYPTED_HEADER_PREFIX)) {
      "Invalid encrypted backup format (missing TNENC1 header)"
    }

    val base64Body = trimmed.removePrefix(ENCRYPTED_HEADER_PREFIX).trim()
    val combined = Base64.getDecoder().decode(base64Body)
    require(combined.size > SALT_LENGTH_BYTES + IV_LENGTH_BYTES + 16) {
      "Corrupted encrypted backup payload"
    }

    val buffer = ByteBuffer.wrap(combined)
    val salt = ByteArray(SALT_LENGTH_BYTES).also { buffer.get(it) }
    val iv = ByteArray(IV_LENGTH_BYTES).also { buffer.get(it) }
    val cipherBytes = ByteArray(buffer.remaining()).also { buffer.get(it) }

    val secretKey = deriveAesKey(passphrase, salt)
    val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

    val plainBytes = cipher.doFinal(cipherBytes)
    return String(plainBytes, StandardCharsets.UTF_8)
  }

  private fun deriveAesKey(passphrase: String, salt: ByteArray): SecretKey {
    val keySpec = PBEKeySpec(
      passphrase.toCharArray(),
      salt,
      ITERATION_COUNT,
      KEY_LENGTH_BITS
    )
    return try {
      val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
      val rawKey = factory.generateSecret(keySpec).encoded
      SecretKeySpec(rawKey, KEY_ALGORITHM)
    } finally {
      keySpec.clearPassword()
    }
  }
}

package com.example.data

import com.example.data.backup.BackupCryptoManager
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoManagerTest {

  private val cryptoManager = BackupCryptoManager()

  @Test
  fun `encrypt and decrypt roundtrip preserves UTF-8 JSON payload`() {
    val sampleJson = """{"schemaVersion":1,"lists":[{"name":"Personal 🚀","colorHex":4282339765}]}"""
    val passphrase = "correct-horse-battery-staple"

    val encrypted = cryptoManager.encrypt(sampleJson, passphrase)
    assertTrue(cryptoManager.isEncryptedPayload(encrypted))
    assertFalse(encrypted.contains("Personal"))

    val decrypted = cryptoManager.decrypt(encrypted, passphrase)
    assertEquals(sampleJson, decrypted)
  }

  @Test
  fun `encrypting identical plaintext twice produces distinct ciphertexts via random salt and IV`() {
    val payload = """{"tasks":[]}"""
    val passphrase = "my-secret-passphrase"

    val first = cryptoManager.encrypt(payload, passphrase)
    val second = cryptoManager.encrypt(payload, passphrase)

    assertNotEquals(first, second)
    assertEquals(payload, cryptoManager.decrypt(first, passphrase))
    assertEquals(payload, cryptoManager.decrypt(second, passphrase))
  }

  @Test(expected = AEADBadTagException::class)
  fun `decrypting with wrong passphrase throws AEADBadTagException`() {
    val encrypted = cryptoManager.encrypt("Sensitive offline task backup", "passphrase-alpha")
    cryptoManager.decrypt(encrypted, "passphrase-beta")
  }
}

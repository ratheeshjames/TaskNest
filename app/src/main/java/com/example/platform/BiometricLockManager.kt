package com.example.platform

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles biometric capability checks, showing [BiometricPrompt] on [AppCompatActivity],
 * and verifying the fallback SHA-256 PIN hash for TaskNest's offline App Lock.
 */
@Singleton
class BiometricLockManager @Inject constructor(
  @ApplicationContext private val context: Context
) {
  fun canAuthenticateWithBiometrics(): Boolean {
    val manager = BiometricManager.from(context)
    val result = manager.canAuthenticate(
      BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.BIOMETRIC_WEAK
    )
    return result == BiometricManager.BIOMETRIC_SUCCESS
  }

  fun hashPin(rawPin: String): String {
    val clean = rawPin.trim()
    if (clean.isEmpty()) return ""
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest("tasknest_pin_salt_v1:$clean".toByteArray(StandardCharsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun verifyPin(rawPin: String, expectedPinHash: String): Boolean {
    if (expectedPinHash.isBlank()) return false
    return hashPin(rawPin) == expectedPinHash
  }

  fun showBiometricPrompt(
    activity: AppCompatActivity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
      activity,
      executor,
      object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
          super.onAuthenticationSucceeded(result)
          onSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
          super.onAuthenticationError(errorCode, errString)
          onError(errString.toString())
        }
      }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle("Unlock TaskNest")
      .setSubtitle("Authenticate to access your private offline tasks")
      .setNegativeButtonText("Use PIN")
      .setAllowedAuthenticators(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
          BiometricManager.Authenticators.BIOMETRIC_WEAK
      )
      .build()

    prompt.authenticate(promptInfo)
  }
}

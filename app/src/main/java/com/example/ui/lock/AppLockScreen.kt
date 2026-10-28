package com.example.ui.lock

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.platform.BiometricLockManager

/**
 * Full-screen App Lock gate supporting both Android BiometricPrompt and local PIN fallback.
 */
@Composable
fun AppLockScreen(
  biometricLockManager: BiometricLockManager,
  expectedPinHash: String,
  onUnlocked: () -> Unit
) {
  val context = LocalContext.current
  var pinInput by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val canUseBiometrics = remember { biometricLockManager.canAuthenticateWithBiometrics() }

  LaunchedEffect(canUseBiometrics) {
    if (canUseBiometrics) {
      (context as? AppCompatActivity)?.let { activity ->
        biometricLockManager.showBiometricPrompt(
          activity = activity,
          onSuccess = onUnlocked,
          onError = { /* User can fall back to PIN */ }
        )
      }
    }
  }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier
      .fillMaxSize()
      .testTag("app_lock_screen")
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp)
    ) {
      Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer)
          ) {
            Icon(
              imageVector = Icons.Filled.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(34.dp)
            )
          }

          Text(
            text = "TaskNest is Locked",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
          )

          Text(
            text = "Unlock with your PIN or biometrics to view your private tasks.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )

          if (expectedPinHash.isNotBlank()) {
            OutlinedTextField(
              value = pinInput,
              onValueChange = {
                pinInput = it.filter { ch -> ch.isDigit() }.take(8)
                errorMessage = null
                if (biometricLockManager.verifyPin(pinInput, expectedPinHash)) {
                  onUnlocked()
                }
              },
              label = { Text("Enter PIN") },
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("app_lock_pin_input")
            )

            Button(
              onClick = {
                if (biometricLockManager.verifyPin(pinInput, expectedPinHash)) {
                  onUnlocked()
                } else {
                  errorMessage = "Incorrect PIN. Please try again."
                }
              },
              enabled = pinInput.length >= 4,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("app_lock_unlock_button")
            ) {
              Text("Unlock")
            }
          }

          if (errorMessage != null) {
            Text(
              text = errorMessage!!,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error
            )
          }

          if (canUseBiometrics) {
            FilledTonalButton(
              onClick = {
                (context as? AppCompatActivity)?.let { activity ->
                  biometricLockManager.showBiometricPrompt(
                    activity = activity,
                    onSuccess = onUnlocked,
                    onError = { msg -> errorMessage = msg }
                  )
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("app_lock_biometric_button")
            ) {
              Icon(imageVector = Icons.Filled.Fingerprint, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Unlock with Biometrics")
            }
          } else if (expectedPinHash.isBlank()) {
            // Safety escape hatch if lock was toggled without setting a PIN on a device with no biometrics
            Spacer(modifier = Modifier.height(4.dp))
            Button(
              onClick = onUnlocked,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Continue to TaskNest")
            }
          }
        }
      }
    }
  }
}

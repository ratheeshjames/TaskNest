package com.example.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class FaqEntry(
  val question: String,
  val answer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpFaqScreen(
  viewModel: SettingsViewModel,
  onNavigateBack: () -> Unit,
  onReplayOnboarding: () -> Unit
) {
  val faqs = remember {
    listOf(
      FaqEntry(
        question = "Does TaskNest ever connect to the internet?",
        answer = "Never. TaskNest declares zero network permissions in its AndroidManifest. All tasks, lists, attachments, and analytics stay 100% local on your device."
      ),
      FaqEntry(
        question = "How does Natural-Language Quick Add work?",
        answer = "Type phrases like \"Call dentist tomorrow 3pm\", \"Pay rent next monday\", or \"Submit taxes in 3 days at 09:30\" into the Quick-Add bar or Task Editor. TaskNest automatically extracts the due date and time."
      ),
      FaqEntry(
        question = "Why didn't my reminder ring on time?",
        answer = "Ensure Notifications and Exact Alarms are enabled in Settings, and consider exempting TaskNest from aggressive battery optimization (Doze mode) if your device manufacturer restricts background alarms."
      ),
      FaqEntry(
        question = "How do encrypted backups work?",
        answer = "Encrypted backups (.tnbak) use PBKDF2-HMAC-SHA256 (65,536 iterations, 16-byte random salt) to derive a 256-bit key and encrypt your JSON snapshot with AES/GCM/NoPadding."
      ),
      FaqEntry(
        question = "How long do deleted tasks stay in Trash?",
        answer = "Soft-deleted tasks remain in Trash for 30 days so you can restore them anytime before automatic permanent cleanup."
      )
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        },
        title = {
          Text(
            text = "Help, FAQ & Privacy",
            style = MaterialTheme.typography.titleLarge
          )
        }
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Card(
          shape = MaterialTheme.shapes.extraLarge,
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = "TaskNest 1.0 • Offline-First Guarantee",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
            Text(
              text = "Built with Kotlin, Jetpack Compose Material 3 Expressive, Room, DataStore, and Glance. Zero ads, zero telemetry, zero cloud lock-in.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            FilledTonalButton(
              onClick = {
                viewModel.replayOnboarding(onReady = onReplayOnboarding)
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("replay_onboarding_button")
            ) {
              Icon(imageVector = Icons.Filled.Replay, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Replay Onboarding & Coach Marks")
            }
          }
        }
      }

      items(faqs) { faq ->
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = faq.question,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = faq.answer,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

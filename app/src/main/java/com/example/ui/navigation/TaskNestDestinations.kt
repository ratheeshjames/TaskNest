package com.example.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose route definitions for TaskNest.
 */
@Serializable
object OnboardingRoute

@Serializable
object HomeRoute

@Serializable
object SearchRoute

@Serializable
object ViewsHubRoute

@Serializable
object StatsRoute

@Serializable
object SettingsRoute

@Serializable
data class TaskEditorRoute(
  val taskId: Long = -1L,
  val initialTitleSeed: String = "",
  val initialListId: Long = -1L
)

@Serializable
object PomodoroRoute

@Serializable
object TrashRoute

@Serializable
object BackupRestoreRoute

@Serializable
object HelpFaqRoute

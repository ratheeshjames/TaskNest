package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.domain.model.SmartListType
import com.example.platform.IncomingShareOrShortcutRequest
import com.example.ui.backup.BackupRestoreScreen
import com.example.ui.backup.BackupViewModel
import com.example.ui.editor.TaskEditorScreen
import com.example.ui.editor.TaskEditorViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.onboarding.OnboardingViewModel
import com.example.ui.search.SearchAndFilterScreen
import com.example.ui.search.SearchViewModel
import com.example.ui.settings.HelpFaqScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.stats.PomodoroScreen
import com.example.ui.stats.StatsAndReviewScreen
import com.example.ui.stats.StatsViewModel
import com.example.ui.trash.TrashScreen
import com.example.ui.trash.TrashViewModel
import com.example.ui.views.AgendaScreen
import com.example.ui.views.CalendarScreen
import com.example.ui.views.KanbanBoardScreen

private data class TopLevelDestination<T : Any>(
  val route: T,
  val label: String,
  val icon: ImageVector,
  val testTag: String
)

@Composable
fun TaskNestNavHost(
  onboardingCompleted: Boolean,
  isExpandedScreen: Boolean,
  incomingRequest: IncomingShareOrShortcutRequest?,
  onConsumeIncomingRequest: () -> Unit,
  modifier: Modifier = Modifier
) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination

  val topLevelDestinations = listOf(
    TopLevelDestination(HomeRoute, "Tasks", Icons.Filled.CheckCircle, "nav_tasks"),
    TopLevelDestination(SearchRoute, "Search", Icons.Filled.Search, "nav_search"),
    TopLevelDestination(ViewsHubRoute, "Views", Icons.Filled.CalendarMonth, "nav_views"),
    TopLevelDestination(StatsRoute, "Stats", Icons.Filled.BarChart, "nav_stats"),
    TopLevelDestination(SettingsRoute, "Settings", Icons.Filled.Settings, "nav_settings")
  )

  val showNavigationChrome = currentDestination != null &&
    topLevelDestinations.any { currentDestination.hasRoute(it.route::class) }

  // Handle deep-links, shortcuts, widgets, Quick Settings tile, and ACTION_SEND share targets
  LaunchedEffect(incomingRequest, onboardingCompleted) {
    val req = incomingRequest ?: return@LaunchedEffect
    if (!onboardingCompleted) return@LaunchedEffect

    when {
      req.openTaskId != null -> {
        navController.navigate(
          TaskEditorRoute(
            taskId = req.openTaskId,
            initialTitleSeed = "",
            initialListId = -1L
          )
        )
      }
      req.openQuickAddEditor -> {
        val combinedSeed = if (req.sharedNotes.isNotBlank()) {
          "${req.sharedTitle} — ${req.sharedNotes}"
        } else {
          req.sharedTitle
        }
        navController.navigate(
          TaskEditorRoute(
            taskId = -1L,
            initialTitleSeed = combinedSeed,
            initialListId = -1L
          )
        )
      }
      req.jumpToPomodoro -> {
        navController.navigate(PomodoroRoute)
      }
      req.jumpToToday -> {
        navController.navigate(HomeRoute) {
          popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
          }
          launchSingleTop = true
          restoreState = true
        }
      }
    }
    onConsumeIncomingRequest()
  }

  val startDestination: Any = if (onboardingCompleted) HomeRoute else OnboardingRoute

  Scaffold(
    bottomBar = {
      if (showNavigationChrome && !isExpandedScreen) {
        NavigationBar {
          topLevelDestinations.forEach { dest ->
            val selected = currentDestination?.hasRoute(dest.route::class) == true
            NavigationBarItem(
              selected = selected,
              onClick = {
                navController.navigate(dest.route) {
                  popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                  }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = { Icon(imageVector = dest.icon, contentDescription = dest.label) },
              label = { Text(dest.label) },
              modifier = Modifier.testTag(dest.testTag)
            )
          }
        }
      }
    },
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (showNavigationChrome && isExpandedScreen) {
        NavigationRail(modifier = Modifier.fillMaxHeight()) {
          topLevelDestinations.forEach { dest ->
            val selected = currentDestination?.hasRoute(dest.route::class) == true
            NavigationRailItem(
              selected = selected,
              onClick = {
                navController.navigate(dest.route) {
                  popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                  }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = { Icon(imageVector = dest.icon, contentDescription = dest.label) },
              label = { Text(dest.label) },
              modifier = Modifier.testTag(dest.testTag)
            )
          }
        }
      }

      Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
        NavHost(
          navController = navController,
          startDestination = startDestination
        ) {
          composable<OnboardingRoute> {
            val viewModel: OnboardingViewModel = hiltViewModel()
            OnboardingScreen(
              viewModel = viewModel,
              onFinished = {
                navController.navigate(HomeRoute) {
                  popUpTo(OnboardingRoute) { inclusive = true }
                }
              }
            )
          }

          composable<HomeRoute> {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
              viewModel = viewModel,
              isExpandedScreen = isExpandedScreen,
              onOpenTaskEditor = { taskId, titleSeed, listId ->
                navController.navigate(
                  TaskEditorRoute(
                    taskId = taskId ?: -1L,
                    initialTitleSeed = titleSeed ?: "",
                    initialListId = listId ?: -1L
                  )
                )
              },
              onOpenSearch = {
                navController.navigate(SearchRoute)
              },
              onOpenPomodoro = {
                navController.navigate(PomodoroRoute)
              },
              onOpenTrash = {
                navController.navigate(TrashRoute)
              }
            )
          }

          composable<SearchRoute> {
            val viewModel: SearchViewModel = hiltViewModel()
            SearchAndFilterScreen(
              viewModel = viewModel,
              onOpenTask = { taskId ->
                navController.navigate(TaskEditorRoute(taskId = taskId))
              }
            )
          }

          composable<ViewsHubRoute> {
            val statsViewModel: StatsViewModel = hiltViewModel()
            ViewsSwitcherContainer(
              viewModel = statsViewModel,
              onOpenTask = { taskId ->
                navController.navigate(TaskEditorRoute(taskId = taskId))
              },
              onCreateTaskForDate = { date ->
                navController.navigate(
                  TaskEditorRoute(
                    taskId = -1L,
                    initialTitleSeed = "on $date"
                  )
                )
              }
            )
          }

          composable<StatsRoute> {
            val statsViewModel: StatsViewModel = hiltViewModel()
            StatsAndReviewScreen(
              viewModel = statsViewModel,
              onOpenPomodoro = { navController.navigate(PomodoroRoute) },
              onOpenTask = { taskId ->
                navController.navigate(TaskEditorRoute(taskId = taskId))
              }
            )
          }

          composable<SettingsRoute> {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
              viewModel = settingsViewModel,
              onOpenBackupRestore = { navController.navigate(BackupRestoreRoute) },
              onOpenTrash = { navController.navigate(TrashRoute) },
              onOpenHelpFaq = { navController.navigate(HelpFaqRoute) }
            )
          }

          composable<TaskEditorRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TaskEditorRoute>()
            val editorViewModel: TaskEditorViewModel = hiltViewModel()
            TaskEditorScreen(
              viewModel = editorViewModel,
              taskId = route.taskId.takeIf { it > 0L },
              initialTitleSeed = route.initialTitleSeed.takeIf { it.isNotBlank() },
              initialListId = route.initialListId.takeIf { it > 0L },
              onNavigateBack = { navController.popBackStack() }
            )
          }

          composable<PomodoroRoute> {
            val statsViewModel: StatsViewModel = hiltViewModel()
            PomodoroScreen(
              viewModel = statsViewModel,
              onNavigateBack = { navController.popBackStack() }
            )
          }

          composable<TrashRoute> {
            val trashViewModel: TrashViewModel = hiltViewModel()
            TrashScreen(
              viewModel = trashViewModel,
              onNavigateBack = { navController.popBackStack() }
            )
          }

          composable<BackupRestoreRoute> {
            val backupViewModel: BackupViewModel = hiltViewModel()
            BackupRestoreScreen(
              viewModel = backupViewModel,
              onNavigateBack = { navController.popBackStack() }
            )
          }

          composable<HelpFaqRoute> {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            HelpFaqScreen(
              viewModel = settingsViewModel,
              onNavigateBack = { navController.popBackStack() },
              onReplayOnboarding = {
                navController.navigate(OnboardingRoute) {
                  popUpTo(HomeRoute) { inclusive = true }
                }
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ViewsSwitcherContainer(
  viewModel: StatsViewModel,
  onOpenTask: (Long) -> Unit,
  onCreateTaskForDate: (java.time.LocalDate) -> Unit
) {
  var selectedSubView by rememberSaveable { mutableIntStateOf(0) }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      FilterChip(
        selected = selectedSubView == 0,
        onClick = { selectedSubView = 0 },
        label = { Text("Calendar") },
        leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
        modifier = Modifier
          .padding(end = 8.dp)
          .testTag("subview_calendar_chip")
      )
      FilterChip(
        selected = selectedSubView == 1,
        onClick = { selectedSubView = 1 },
        label = { Text("Kanban") },
        leadingIcon = { Icon(Icons.Filled.ViewKanban, contentDescription = null) },
        modifier = Modifier
          .padding(end = 8.dp)
          .testTag("subview_kanban_chip")
      )
      FilterChip(
        selected = selectedSubView == 2,
        onClick = { selectedSubView = 2 },
        label = { Text("Agenda") },
        leadingIcon = { Icon(Icons.Filled.ViewAgenda, contentDescription = null) },
        modifier = Modifier.testTag("subview_agenda_chip")
      )
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when (selectedSubView) {
        0 -> CalendarScreen(
          viewModel = viewModel,
          onOpenTask = onOpenTask,
          onCreateTaskForDate = onCreateTaskForDate
        )
        1 -> KanbanBoardScreen(
          viewModel = viewModel,
          onOpenTask = onOpenTask
        )
        else -> AgendaScreen(
          viewModel = viewModel,
          onOpenTask = onOpenTask
        )
      }
    }
  }
}

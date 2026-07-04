package com.productivity.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.productivity.app.ui.checklist.ChecklistListScreen
import com.productivity.app.ui.checklist.CreateChecklistScreen
import com.productivity.app.ui.checklist.ChecklistDetailScreen
import com.productivity.app.ui.dashboard.DashboardScreen
import com.productivity.app.ui.notes.NoteLogListScreen
import com.productivity.app.ui.notes.CreateNoteLogScreen
import com.productivity.app.ui.reminders.CreateReminderScreen
import com.productivity.app.ui.reminders.ReminderDetailScreen
import com.productivity.app.ui.reminders.ReminderListScreen
import com.productivity.app.ui.schedule.CreateEventScreen
import com.productivity.app.ui.schedule.EventDetailScreen
import com.productivity.app.ui.schedule.ScheduleListScreen
import com.productivity.app.ui.settings.AlarmSettingsScreen
import com.productivity.app.ui.tracker.CreateTrackerScreen
import com.productivity.app.ui.tracker.TrackerDetailScreen
import com.productivity.app.ui.tracker.TrackerListScreen
import com.productivity.app.ui.statistics.StatisticsScreen
import com.productivity.app.ui.focus.FocusListScreen
import com.productivity.app.ui.focus.FocusTimerScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    onOpenDrawer: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        // ── Dashboard ────────────────────────────────────────
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToReminders = {
                    navController.navigate(Screen.Reminders.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSchedule = {
                    navController.navigate(Screen.Schedule.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToTrackers = {
                    navController.navigate(Screen.Trackers.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToReminderDetail = { reminderId ->
                    navController.navigate(Screen.ReminderDetail.createRoute(reminderId))
                },
                onNavigateToEventDetail = { eventId ->
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                },
                onNavigateToTrackerDetail = { trackerId ->
                    navController.navigate(Screen.TrackerDetail.createRoute(trackerId))
                },
                onOpenDrawer = onOpenDrawer
            )
        }

        // ── Reminders ────────────────────────────────────────
        composable(Screen.Reminders.route) {
            ReminderListScreen(
                onNavigateToCreate = {
                    navController.navigate(Screen.CreateReminder.route)
                },
                onNavigateToDetail = { reminderId ->
                    navController.navigate(Screen.ReminderDetail.createRoute(reminderId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.AlarmSettings.route)
                },
                onOpenDrawer = onOpenDrawer
            )
        }
        composable(Screen.CreateReminder.route) {
            CreateReminderScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.ReminderDetail.route,
            arguments = listOf(
                navArgument("reminderId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getLong("reminderId") ?: return@composable
            ReminderDetailScreen(
                reminderId = reminderId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Alarm Settings ───────────────────────────────────
        composable(Screen.AlarmSettings.route) {
            AlarmSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Schedule ─────────────────────────────────────────
        composable(Screen.Schedule.route) {
            ScheduleListScreen(
                onNavigateToCreate = {
                    navController.navigate(Screen.CreateEvent.route)
                },
                onNavigateToDetail = { eventId ->
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                },
                onOpenDrawer = onOpenDrawer
            )
        }
        composable(Screen.CreateEvent.route) {
            CreateEventScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EventDetail.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: return@composable
            EventDetailScreen(
                eventId = eventId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Trackers ─────────────────────────────────────────
        composable(Screen.Trackers.route) {
            TrackerListScreen(
                onNavigateToCreate = {
                    navController.navigate(Screen.CreateTracker.route)
                },
                onNavigateToDetail = { trackerId ->
                    navController.navigate(Screen.TrackerDetail.createRoute(trackerId))
                },
                onOpenDrawer = onOpenDrawer
            )
        }
        composable(Screen.CreateTracker.route) {
            CreateTrackerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.TrackerDetail.route,
            arguments = listOf(
                navArgument("trackerId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val trackerId = backStackEntry.arguments?.getLong("trackerId") ?: return@composable
            TrackerDetailScreen(
                trackerId = trackerId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Notes & Checklists ───────────────────────────────
        composable(Screen.Notes.route) {
            NoteLogListScreen(
                onNavigateToCreate = {
                    navController.navigate(Screen.CreateNoteLog.route)
                },
                onOpenDrawer = onOpenDrawer
            )
        }
        composable(Screen.CreateNoteLog.route) {
            CreateNoteLogScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Checklists.route) {
            ChecklistListScreen(
                onNavigateToCreate = {
                    navController.navigate(Screen.CreateChecklist.route)
                },
                onNavigateToDetail = { checklistId ->
                    navController.navigate(Screen.ChecklistDetail.createRoute(checklistId))
                }
            )
        }
        composable(Screen.CreateChecklist.route) {
            CreateChecklistScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.ChecklistDetail.route,
            arguments = listOf(
                navArgument("checklistId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val checklistId = backStackEntry.arguments?.getLong("checklistId") ?: return@composable
            ChecklistDetailScreen(
                checklistId = checklistId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Statistics ───────────────────────────────────────
        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onNavigateToTrackerDetail = { trackerId ->
                    navController.navigate(Screen.TrackerDetail.createRoute(trackerId))
                },
                onOpenDrawer = onOpenDrawer
            )
        }

        // ── Focus ───────────────────────────────────────────
        composable(Screen.FocusList.route) {
            FocusListScreen(
                onNavigateToTimer = { taskId ->
                    navController.navigate(Screen.FocusTimer.createRoute(taskId))
                },
                onOpenDrawer = onOpenDrawer
            )
        }
        composable(
            route = Screen.FocusTimer.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: return@composable
            FocusTimerScreen(
                taskId = taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

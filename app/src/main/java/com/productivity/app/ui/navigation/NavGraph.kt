package com.productivity.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.productivity.app.ui.checklist.ChecklistListScreen
import com.productivity.app.ui.dashboard.DashboardScreen
import com.productivity.app.ui.notes.NoteLogListScreen
import com.productivity.app.ui.reminders.CreateReminderScreen
import com.productivity.app.ui.reminders.ReminderDetailScreen
import com.productivity.app.ui.reminders.ReminderListScreen
import com.productivity.app.ui.schedule.CreateEventScreen
import com.productivity.app.ui.schedule.EventDetailScreen
import com.productivity.app.ui.schedule.ScheduleListScreen
import com.productivity.app.ui.tracker.CreateTrackerScreen
import com.productivity.app.ui.tracker.TrackerDetailScreen
import com.productivity.app.ui.tracker.TrackerListScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        // ── Dashboard ────────────────────────────────────────
        composable(Screen.Dashboard.route) {
            DashboardScreen()
        }

        // ── Reminders ────────────────────────────────────────
        composable(Screen.Reminders.route) {
            ReminderListScreen(
                onNavigateToCreate = {
                    navController.navigate(Screen.CreateReminder.route)
                },
                onNavigateToDetail = { reminderId ->
                    navController.navigate(Screen.ReminderDetail.createRoute(reminderId))
                }
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

        // ── Schedule ─────────────────────────────────────────
        composable(Screen.Schedule.route) {
            ScheduleListScreen(
                onNavigateToCreate = {
                    navController.navigate(Screen.CreateEvent.route)
                },
                onNavigateToDetail = { eventId ->
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                }
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
                }
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

        // ── Notes & Checklists (still placeholders) ──────────
        composable(Screen.Notes.route) {
            NoteLogListScreen()
        }
        composable(Screen.Checklists.route) {
            ChecklistListScreen()
        }
    }
}

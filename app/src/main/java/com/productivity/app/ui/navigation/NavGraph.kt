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
import com.productivity.app.ui.schedule.ScheduleListScreen
import com.productivity.app.ui.tracker.TrackerListScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen()
        }
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
        composable(Screen.Schedule.route) {
            ScheduleListScreen()
        }
        composable(Screen.Trackers.route) {
            TrackerListScreen()
        }
        composable(Screen.Notes.route) {
            NoteLogListScreen()
        }
        composable(Screen.Checklists.route) {
            ChecklistListScreen()
        }
    }
}

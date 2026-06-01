package com.productivity.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : Screen(
        route = "dashboard",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Reminders : Screen(
        route = "reminders",
        title = "Reminders",
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications
    )

    data object Schedule : Screen(
        route = "schedule",
        title = "Schedule",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    )

    data object Trackers : Screen(
        route = "trackers",
        title = "Trackers",
        selectedIcon = Icons.Filled.TrendingUp,
        unselectedIcon = Icons.Outlined.TrendingUp
    )

    data object Notes : Screen(
        route = "notes",
        title = "Notes",
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description
    )

    data object Checklists : Screen(
        route = "checklists",
        title = "Checklists",
        selectedIcon = Icons.Filled.Checklist,
        unselectedIcon = Icons.Outlined.Checklist
    )

    companion object {
        /** Items displayed in the bottom navigation bar */
        val bottomNavItems = listOf(Dashboard, Reminders, Schedule, Trackers, Notes)
    }
}

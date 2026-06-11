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

    /** Non-bottom-nav screens for reminder CRUD */
    data object CreateReminder : Screen(
        route = "reminders/create",
        title = "New Reminder",
        selectedIcon = Icons.Filled.AddAlert,
        unselectedIcon = Icons.Outlined.AddAlert
    )

    data object ReminderDetail : Screen(
        route = "reminders/{reminderId}",
        title = "Reminder Detail",
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications
    ) {
        fun createRoute(reminderId: Long) = "reminders/$reminderId"
    }

    /** Non-bottom-nav screens for schedule CRUD */
    data object CreateEvent : Screen(
        route = "schedule/create",
        title = "New Event",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    )

    data object EventDetail : Screen(
        route = "schedule/{eventId}",
        title = "Event Detail",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    ) {
        fun createRoute(eventId: Long) = "schedule/$eventId"
    }

    /** Non-bottom-nav screens for tracker CRUD */
    data object CreateTracker : Screen(
        route = "trackers/create",
        title = "New Tracker",
        selectedIcon = Icons.Filled.TrendingUp,
        unselectedIcon = Icons.Outlined.TrendingUp
    )

    data object TrackerDetail : Screen(
        route = "trackers/{trackerId}",
        title = "Tracker Detail",
        selectedIcon = Icons.Filled.TrendingUp,
        unselectedIcon = Icons.Outlined.TrendingUp
    ) {
        fun createRoute(trackerId: Long) = "trackers/$trackerId"
    }

    /** Non-bottom-nav screens for checklist CRUD */
    data object CreateChecklist : Screen(
        route = "checklists/create",
        title = "New Checklist",
        selectedIcon = Icons.Filled.Checklist,
        unselectedIcon = Icons.Outlined.Checklist
    )

    data object ChecklistDetail : Screen(
        route = "checklists/{checklistId}",
        title = "Checklist Detail",
        selectedIcon = Icons.Filled.Checklist,
        unselectedIcon = Icons.Outlined.Checklist
    ) {
        fun createRoute(checklistId: Long) = "checklists/$checklistId"
    }

    /** Non-bottom-nav screens for note logs CRUD */
    data object CreateNoteLog : Screen(
        route = "notes/create",
        title = "New Note Log",
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description
    )

    /** Alarm settings screen — accessed from Reminders top bar */
    data object AlarmSettings : Screen(
        route = "alarm_settings",
        title = "Alarm Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object Statistics : Screen(
        route = "statistics",
        title = "Stats",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    )

    data object PersonalManager : Screen(
        route = "personal_manager",
        title = "Personal Manager",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    data object AlarmActive : Screen(
        route = "alarm_active?reminderId={reminderId}&eventId={eventId}",
        title = "Active Alarm",
        selectedIcon = Icons.Filled.Alarm,
        unselectedIcon = Icons.Outlined.Alarm
    ) {
        fun createRoute(reminderId: Long?, eventId: Long?) =
            "alarm_active?reminderId=${reminderId ?: -1L}&eventId=${eventId ?: -1L}"
    }

    companion object {
        /** Items displayed in the bottom navigation bar */
        val bottomNavItems = listOf(Dashboard, Reminders, Schedule, Trackers, Statistics, Notes)
    }
}

package com.productivity.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productivity.app.data.model.ScheduleEvent
import com.productivity.app.ui.common.DashboardProgressCard
import com.productivity.app.ui.common.DashboardReminderCard
import com.productivity.app.ui.common.SectionHeader
import com.productivity.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToReminders: () -> Unit = {},
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToTrackers: () -> Unit = {},
    onNavigateToReminderDetail: (Long) -> Unit = {},
    onNavigateToEventDetail: (Long) -> Unit = {},
    onNavigateToTrackerDetail: (Long) -> Unit = {},
    onNavigateToPersonalManager: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val greeting = remember { viewModel.getGreeting() }
    val todayFormatted = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AccentPrimary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // ── Greeting Header ─────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = todayFormatted,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
            IconButton(
                onClick = onNavigateToPersonalManager,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface),
                colors = IconButtonDefaults.iconButtonColors(contentColor = AccentPrimary)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Personal Manager",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Quick Stats Row ─────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickStatCard(
                label = "Reminders",
                value = "${state.todayReminders.size}",
                icon = Icons.Outlined.NotificationsActive,
                color = MedicineColor,
                modifier = Modifier.weight(1f)
            )
            QuickStatCard(
                label = "Events",
                value = "${state.upcomingEvents.size}",
                icon = Icons.Outlined.CalendarMonth,
                color = MeetingColor,
                modifier = Modifier.weight(1f)
            )
            QuickStatCard(
                label = "Trackers",
                value = "${state.activeTrackers.size}",
                icon = Icons.Outlined.TrendingUp,
                color = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Today's Reminders ───────────────────────────────────
        SectionHeader(
            title = "Today's Reminders",
            icon = Icons.Outlined.NotificationsActive,
            actionLabel = if (state.todayReminders.isNotEmpty()) "View All" else null,
            onAction = onNavigateToReminders
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (state.todayReminders.isEmpty()) {
            DashboardEmptyCard(
                icon = Icons.Outlined.NotificationsNone,
                message = "No reminders for today"
            )
        } else {
            state.todayReminders.forEachIndexed { index, reminder ->
                DashboardReminderCard(
                    reminder = reminder,
                    onClick = { onNavigateToReminderDetail(reminder.id) }
                )
                if (index < state.todayReminders.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Upcoming Events ─────────────────────────────────────
        SectionHeader(
            title = "Upcoming Events",
            icon = Icons.Outlined.CalendarMonth,
            actionLabel = if (state.upcomingEvents.isNotEmpty()) "View All" else null,
            onAction = onNavigateToSchedule
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (state.upcomingEvents.isEmpty()) {
            DashboardEmptyCard(
                icon = Icons.Outlined.EventBusy,
                message = "No upcoming events this week"
            )
        } else {
            state.upcomingEvents.forEachIndexed { index, event ->
                DashboardEventCard(
                    event = event,
                    onClick = { onNavigateToEventDetail(event.id) }
                )
                if (index < state.upcomingEvents.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Active Trackers ─────────────────────────────────────
        SectionHeader(
            title = "Active Trackers",
            icon = Icons.Outlined.TrendingUp,
            actionLabel = if (state.activeTrackers.isNotEmpty()) "View All" else null,
            onAction = onNavigateToTrackers
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (state.activeTrackers.isEmpty()) {
            DashboardEmptyCard(
                icon = Icons.Outlined.BarChart,
                message = "No active trackers"
            )
        } else {
            state.activeTrackers.forEachIndexed { index, tracker ->
                DashboardProgressCard(
                    tracker = tracker,
                    onClick = { onNavigateToTrackerDetail(tracker.id) }
                )
                if (index < state.activeTrackers.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Quick Stat Card ─────────────────────────────────────────────────

@Composable
private fun QuickStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

// ── Dashboard Event Card ────────────────────────────────────────────

@Composable
private fun DashboardEventCard(
    event: ScheduleEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeColor = getEventTypeColor(event.type)
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getEventTypeIcon(event.type),
                    contentDescription = event.type,
                    tint = typeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormatter.format(Date(event.startDatetime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    if (!event.isAllDay) {
                        Text(
                            text = " · ${timeFormatter.format(Date(event.startDatetime))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    } else {
                        Text(
                            text = " · All day",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
                if (!event.location.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── Dashboard Empty Card ────────────────────────────────────────────

@Composable
private fun DashboardEmptyCard(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary
            )
        }
    }
}

// ── Event Helpers ───────────────────────────────────────────────────

private fun getEventTypeColor(type: String): Color = when (type.lowercase()) {
    "meeting" -> MeetingColor
    "appointment" -> GeneralColor
    "deadline" -> DeadlineColor
    else -> AccentPrimary
}

private fun getEventTypeIcon(type: String): ImageVector = when (type.lowercase()) {
    "meeting" -> Icons.Outlined.Groups
    "appointment" -> Icons.Outlined.PersonOutline
    "deadline" -> Icons.Outlined.Timer
    else -> Icons.Outlined.Event
}

package com.productivity.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productivity.app.ui.common.SectionHeader
import com.productivity.app.ui.common.DashboardProgressCard
import com.productivity.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToReminders: () -> Unit = {},
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToTrackers: () -> Unit = {},
    onNavigateToReminderDetail: (Long) -> Unit = {},
    onNavigateToEventDetail: (Long) -> Unit = {},
    onNavigateToTrackerDetail: (Long) -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val greeting = remember { viewModel.getGreeting() }
    val todayFormatted = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    val digestHour by viewModel.digestHour.collectAsStateWithLifecycle()
    val digestMinute by viewModel.digestMinute.collectAsStateWithLifecycle()
    val digestEnabled by viewModel.digestEnabled.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
        Spacer(Modifier.height(20.dp))

        // ── Header ──────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, contentDescription = "Open menu", tint = TextPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = todayFormatted,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Up Next (hero) ──────────────────────────────────────
        state.upNext?.let { next ->
            UpNextCard(
                entry = next,
                onClick = { openEntry(next, onNavigateToReminderDetail, onNavigateToEventDetail) }
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Needs attention (overdue) ───────────────────────────
        if (state.overdue.isNotEmpty()) {
            OverdueBanner(count = state.overdue.size, onClick = onNavigateToReminders)
            Spacer(Modifier.height(16.dp))
        }

        // ── Quick stats ─────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickStat("Reminders", state.todayReminderCount, Icons.Outlined.NotificationsActive, MedicineColor, Modifier.weight(1f), onNavigateToReminders)
            QuickStat("Events", state.todayEventCount, Icons.Outlined.CalendarMonth, MeetingColor, Modifier.weight(1f), onNavigateToSchedule)
            QuickStat("Trackers", state.activeTrackerCount, Icons.Outlined.TrendingUp, SuccessGreen, Modifier.weight(1f), onNavigateToTrackers)
        }

        Spacer(Modifier.height(28.dp))

        // ── Today's agenda ──────────────────────────────────────
        SectionHeader(
            title = "Today's Agenda",
            icon = Icons.Outlined.Today,
            actionLabel = null
        )
        Spacer(Modifier.height(12.dp))
        if (state.agenda.isEmpty()) {
            EmptyCard(Icons.Outlined.EventAvailable, "Your day is clear — nothing scheduled.")
        } else {
            state.agenda.forEachIndexed { index, entry ->
                AgendaRow(
                    entry = entry,
                    onClick = { openEntry(entry, onNavigateToReminderDetail, onNavigateToEventDetail) }
                )
                if (index < state.agenda.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }

        // ── Weekly goals ────────────────────────────────────────
        if (state.weeklyGoals.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            SectionHeader(title = "Weekly Goals", icon = Icons.Outlined.Flag, actionLabel = null)
            Spacer(Modifier.height(12.dp))
            state.weeklyGoals.forEachIndexed { index, (tracker, goal) ->
                WeeklyGoalCard(
                    title = tracker.title,
                    isCourse = tracker.type == "course",
                    achieved = goal.achievedCount,
                    target = goal.targetCount,
                    onClick = { onNavigateToTrackerDetail(tracker.id) }
                )
                if (index < state.weeklyGoals.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }

        // ── Active trackers ─────────────────────────────────────
        Spacer(Modifier.height(28.dp))
        SectionHeader(
            title = "Active Trackers",
            icon = Icons.Outlined.TrendingUp,
            actionLabel = if (state.activeTrackers.isNotEmpty()) "View All" else null,
            onAction = onNavigateToTrackers
        )
        Spacer(Modifier.height(12.dp))
        if (state.activeTrackers.isEmpty()) {
            EmptyCard(Icons.Outlined.BarChart, "No active trackers yet.")
        } else {
            state.activeTrackers.forEachIndexed { index, tracker ->
                DashboardProgressCard(tracker = tracker, onClick = { onNavigateToTrackerDetail(tracker.id) })
                if (index < state.activeTrackers.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }

        // ── Upcoming (next 3 days) ──────────────────────────────
        if (state.upcoming.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            SectionHeader(title = "Coming Up", icon = Icons.Outlined.Upcoming, actionLabel = null)
            Spacer(Modifier.height(12.dp))
            state.upcoming.forEachIndexed { index, entry ->
                UpcomingRow(
                    entry = entry,
                    onClick = { openEntry(entry, onNavigateToReminderDetail, onNavigateToEventDetail) }
                )
                if (index < state.upcoming.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }

        // ── Daily digest footer ─────────────────────────────────
        Spacer(Modifier.height(28.dp))
        DigestSettingRow(
            enabled = digestEnabled,
            time = formatTime(digestHour, digestMinute),
            onToggle = viewModel::updateDigestEnabled,
            onEditTime = { showTimePicker = true }
        )

        Spacer(Modifier.height(32.dp))
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(initialHour = digestHour, initialMinute = digestMinute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Morning digest time", color = TextPrimary) },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = pickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateDigestTime(pickerState.hour, pickerState.minute)
                    showTimePicker = false
                }) { Text("Save", color = AccentPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = DarkSurface
        )
    }
}

// ── Helpers ─────────────────────────────────────────────────────────

private fun openEntry(
    entry: AgendaEntry,
    onReminder: (Long) -> Unit,
    onEvent: (Long) -> Unit
) {
    when (entry) {
        is AgendaEntry.ReminderEntry -> onReminder(entry.id)
        is AgendaEntry.EventEntry -> onEvent(entry.id)
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
    }
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
}

private fun relativeLabel(time: Long): String {
    val diff = time - System.currentTimeMillis()
    if (diff < 0) {
        val past = -diff
        val h = past / 3_600_000
        val m = (past % 3_600_000) / 60_000
        return when {
            h > 0 -> "${h}h ${m}m ago"
            m > 0 -> "${m}m ago"
            else -> "Just now"
        }
    }
    val h = diff / 3_600_000
    val m = (diff % 3_600_000) / 60_000
    return when {
        h > 0 -> "in ${h}h ${m}m"
        m > 0 -> "in ${m}m"
        else -> "now"
    }
}

// ── Up Next hero ────────────────────────────────────────────────────

@Composable
private fun UpNextCard(entry: AgendaEntry, onClick: () -> Unit) {
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val isReminder = entry is AgendaEntry.ReminderEntry
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AccentPrimary.copy(alpha = 0.14f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Bolt, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("UP NEXT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AccentPrimary)
                Spacer(Modifier.weight(1f))
                Text(relativeLabel(entry.time), style = MaterialTheme.typography.labelMedium, color = AccentTertiary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${if (isReminder) "Reminder" else "Event"} · ${timeFormatter.format(Date(entry.time))}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun OverdueBanner(count: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (count == 1) "1 item is overdue" else "$count items are overdue",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
private fun QuickStat(
    label: String,
    value: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text("$value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

// ── Agenda row ──────────────────────────────────────────────────────

@Composable
private fun AgendaRow(entry: AgendaEntry, onClick: () -> Unit) {
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val isReminder = entry is AgendaEntry.ReminderEntry
    val accent = when {
        entry.isCompleted -> TextTertiary
        entry.isOverdue -> ErrorRed
        isReminder -> GeneralColor
        else -> InfoBlue
    }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Time column
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                Text(
                    text = timeFormatter.format(Date(entry.time)),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (entry.isOverdue) ErrorRed else TextSecondary
                )
            }
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (entry.isCompleted) TextTertiary else TextPrimary,
                    textDecoration = if (entry.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        entry.isCompleted -> "Done"
                        entry.isOverdue -> "Overdue · ${relativeLabel(entry.time)}"
                        isReminder -> "Reminder"
                        else -> "Event"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (entry.isOverdue) ErrorRed else TextTertiary
                )
            }
            Icon(
                imageVector = if (isReminder) Icons.Outlined.Notifications else Icons.Outlined.Event,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun UpcomingRow(entry: AgendaEntry, onClick: () -> Unit) {
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()) }
    val isReminder = entry is AgendaEntry.ReminderEntry
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isReminder) Icons.Outlined.Notifications else Icons.Outlined.Event,
                contentDescription = null,
                tint = if (isReminder) GeneralColor else InfoBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = dateFormatter.format(Date(entry.time)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun WeeklyGoalCard(
    title: String,
    isCourse: Boolean,
    achieved: Int,
    target: Int,
    onClick: () -> Unit
) {
    val color = if (isCourse) InfoBlue else AccentTertiary
    val progress = if (target > 0) achieved.toFloat() / target.toFloat() else 0f
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCourse) Icons.Outlined.School else Icons.Outlined.WorkOutline,
                        contentDescription = null, tint = color, modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                Text("$achieved/$target", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AccentPrimary)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = AccentPrimary, trackColor = DarkSurfaceVariant
            )
        }
    }
}

@Composable
private fun DigestSettingRow(
    enabled: Boolean,
    time: String,
    onToggle: (Boolean) -> Unit,
    onEditTime: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(AccentPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Morning digest", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("A daily summary of your day", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBackground,
                        checkedTrackColor = AccentPrimary,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }
            if (enabled) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onEditTime).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Delivered at", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(time, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AccentPrimary)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit time", tint = AccentPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(icon: ImageVector, message: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
        }
    }
}

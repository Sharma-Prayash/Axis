package com.productivity.app.ui.personalmanager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productivity.app.data.model.Reminder
import com.productivity.app.data.model.ScheduleEvent
import com.productivity.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalManagerScreen(
    viewModel: PersonalManagerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToReminderDetail: (Long) -> Unit = {},
    onNavigateToEventDetail: (Long) -> Unit = {},
    onNavigateToTrackerDetail: (Long) -> Unit = {}
) {
    val todayReminders by viewModel.todayReminders.collectAsStateWithLifecycle()
    val todayEvents by viewModel.todayEvents.collectAsStateWithLifecycle()
    val weeklyGoalsProgress by viewModel.weeklyGoalsProgress.collectAsStateWithLifecycle()
    val upcomingItems by viewModel.upcomingItems.collectAsStateWithLifecycle()

    val digestHour by viewModel.digestHour.collectAsStateWithLifecycle()
    val digestMinute by viewModel.digestMinute.collectAsStateWithLifecycle()
    val digestEnabled by viewModel.digestEnabled.collectAsStateWithLifecycle()

    var showTimePickerDialog by remember { mutableStateOf(false) }

    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    // Combine today's reminders and events into a single sorted timeline
    val todayTimeline = remember(todayReminders, todayEvents) {
        val reminderItems = todayReminders.map { TimelineItem.ReminderItem(it) }
        val eventItems = todayEvents.map { TimelineItem.EventItem(it) }
        (reminderItems + eventItems).sortedBy { it.timestamp }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Manager", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Morning Digest Settings ─────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AccentPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LightMode,
                                    contentDescription = null,
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Daily Morning Digest",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Get a daily summary notification",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary
                                )
                            }
                            Switch(
                                checked = digestEnabled,
                                onCheckedChange = { viewModel.updateDigestEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DarkBackground,
                                    checkedTrackColor = AccentPrimary,
                                    uncheckedThumbColor = TextTertiary,
                                    uncheckedTrackColor = DarkSurfaceVariant
                                )
                            )
                        }

                        AnimatedVisibility(visible = digestEnabled) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = DarkSurfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showTimePickerDialog = true }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Digest Schedule Time",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatDigestTime(digestHour, digestMinute),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = "Edit Time",
                                            tint = AccentPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Today's Schedule Timeline ────────────────────────────
            item {
                Text(
                    text = "Today's Agenda",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            if (todayTimeline.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Your agenda is clear for today!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(todayTimeline) { item ->
                    when (item) {
                        is TimelineItem.ReminderItem -> {
                            val timeStr = timeFormatter.format(Date(item.reminder.datetime))
                            Card(
                                onClick = { onNavigateToReminderDetail(item.reminder.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(GeneralColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Notifications,
                                            contentDescription = null,
                                            tint = GeneralColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.reminder.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Reminder • $timeStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                        is TimelineItem.EventItem -> {
                            val timeStr = if (item.event.isAllDay) "All day"
                            else "${timeFormatter.format(Date(item.event.startDatetime))} - ${timeFormatter.format(Date(item.event.endDatetime))}"
                            val typeColor = when (item.event.type.lowercase()) {
                                "meeting" -> MeetingColor
                                "deadline" -> DeadlineColor
                                else -> InfoBlue
                            }
                            Card(
                                onClick = { onNavigateToEventDetail(item.event.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(typeColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (item.event.type.lowercase() == "meeting") Icons.Outlined.Groups else Icons.Outlined.Event,
                                            contentDescription = null,
                                            tint = typeColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.event.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${item.event.type.replaceFirstChar { it.uppercase() }} • $timeStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Active Weekly Goals ──────────────────────────────────
            item {
                Text(
                    text = "Weekly Target Goals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            if (weeklyGoalsProgress.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active tracker weekly goals set.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextTertiary
                            )
                        }
                    }
                }
            } else {
                items(weeklyGoalsProgress) { (tracker, goal) ->
                    val trackerColor = if (tracker.type == "course") InfoBlue else AccentTertiary
                    val goalProgress = if (goal.targetCount > 0) {
                        goal.achievedCount.toFloat() / goal.targetCount.toFloat()
                    } else 0f

                    Card(
                        onClick = { onNavigateToTrackerDetail(tracker.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(trackerColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (tracker.type == "course") Icons.Outlined.School else Icons.Outlined.WorkOutline,
                                        contentDescription = null,
                                        tint = trackerColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = tracker.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${goal.achievedCount}/${goal.targetCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { goalProgress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = AccentPrimary,
                                trackColor = DarkSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Upcoming Deadlines ───────────────────────────────────
            item {
                Text(
                    text = "Upcoming Deadlines (Next 3 Days)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            if (upcomingItems.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No deadlines coming up soon.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextTertiary
                            )
                        }
                    }
                }
            } else {
                items(upcomingItems) { item ->
                    when (item) {
                        is Reminder -> {
                            Card(
                                onClick = { onNavigateToReminderDetail(item.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = null,
                                        tint = GeneralColor
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Due ${dateFormatter.format(Date(item.datetime))} at ${timeFormatter.format(Date(item.datetime))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                        is ScheduleEvent -> {
                            val trackerColor = when (item.type.lowercase()) {
                                "meeting" -> MeetingColor
                                "deadline" -> DeadlineColor
                                else -> InfoBlue
                            }
                            Card(
                                onClick = { onNavigateToEventDetail(item.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (item.type.lowercase() == "meeting") Icons.Outlined.Groups else Icons.Outlined.Event,
                                        contentDescription = null,
                                        tint = trackerColor
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Starts ${dateFormatter.format(Date(item.startDatetime))} at ${timeFormatter.format(Date(item.startDatetime))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = digestHour,
            initialMinute = digestMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text("Select Digest Time", color = TextPrimary) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = DarkSurfaceVariant,
                            clockDialSelectedContentColor = DarkBackground,
                            clockDialUnselectedContentColor = TextPrimary,
                            selectorColor = AccentPrimary,
                            containerColor = DarkSurface,
                            periodSelectorBorderColor = AccentPrimary,
                            periodSelectorSelectedContainerColor = AccentPrimary.copy(alpha = 0.2f),
                            periodSelectorSelectedContentColor = AccentPrimary,
                            periodSelectorUnselectedContainerColor = DarkSurface,
                            periodSelectorUnselectedContentColor = TextSecondary,
                            timeSelectorSelectedContainerColor = AccentPrimary.copy(alpha = 0.2f),
                            timeSelectorSelectedContentColor = AccentPrimary,
                            timeSelectorUnselectedContainerColor = DarkSurfaceVariant,
                            timeSelectorUnselectedContentColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateDigestTime(timePickerState.hour, timePickerState.minute)
                    showTimePickerDialog = false
                }) {
                    Text("Save", color = AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

private sealed class TimelineItem {
    abstract val timestamp: Long
    data class ReminderItem(val reminder: Reminder) : TimelineItem() {
        override val timestamp: Long = reminder.datetime
    }
    data class EventItem(val event: ScheduleEvent) : TimelineItem() {
        override val timestamp: Long = event.startDatetime
    }
}

private fun formatDigestTime(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
}

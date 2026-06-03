package com.productivity.app.ui.schedule

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.productivity.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
    onNavigateToCreate: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {}
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val todaysEvents by viewModel.todaysEvents.collectAsStateWithLifecycle()
    val upcomingEvents by viewModel.upcomingEvents.collectAsStateWithLifecycle()

    // Generate the day selector: today ± 7 days
    val days = remember {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -3)
        (0 until 14).map { i ->
            val dayMillis = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            dayMillis
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = AccentPrimary,
                contentColor = DarkBackground
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Event")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // ── Date Selector ─────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Schedule",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(days) { dayMillis ->
                        DaySelectorItem(
                            dateMillis = dayMillis,
                            isSelected = isSameDay(dayMillis, selectedDate),
                            onClick = { viewModel.setDate(dayMillis) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Today's Agenda ────────────────────────────────
            item {
                val dateFormatter = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
                Text(
                    text = dateFormatter.format(Date(selectedDate)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (todaysEvents.isEmpty()) {
                item {
                    EmptyAgendaCard()
                }
            } else {
                items(todaysEvents, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onClick = { onNavigateToDetail(event.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }

            // ── Upcoming Section ──────────────────────────────
            if (upcomingEvents.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Upcoming (Next 7 Days)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Group upcoming events by date
                val grouped = upcomingEvents.groupBy { event ->
                    val cal = Calendar.getInstance().apply { timeInMillis = event.startDatetime }
                    cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
                }

                grouped.forEach { (_, eventsForDay) ->
                    val dateFormatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                    item {
                        Text(
                            text = dateFormatter.format(Date(eventsForDay.first().startDatetime)),
                            style = MaterialTheme.typography.labelLarge,
                            color = TextTertiary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                    items(eventsForDay, key = { it.id }) { event ->
                        EventCard(
                            event = event,
                            onClick = { onNavigateToDetail(event.id) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Day Selector Chip ───────────────────────────────────────────

@Composable
private fun DaySelectorItem(
    dateMillis: Long,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val calendar = remember(dateMillis) {
        Calendar.getInstance().apply { timeInMillis = dateMillis }
    }
    val dayOfWeek = remember(dateMillis) {
        SimpleDateFormat("EEE", Locale.getDefault()).format(Date(dateMillis)).uppercase()
    }
    val dayOfMonth = remember(dateMillis) {
        calendar.get(Calendar.DAY_OF_MONTH).toString()
    }
    val isToday = remember(dateMillis) {
        isSameDay(dateMillis, System.currentTimeMillis())
    }

    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> AccentPrimary
            else -> DarkSurface
        },
        label = "daySelectorBg"
    )
    val textColor = if (isSelected) DarkBackground else TextPrimary
    val subColor = if (isSelected) DarkBackground.copy(alpha = 0.7f) else TextTertiary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .width(38.dp)
    ) {
        Text(
            text = dayOfWeek,
            style = MaterialTheme.typography.labelSmall,
            color = subColor,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dayOfMonth,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
        if (isToday) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) DarkBackground else AccentPrimary)
            )
        }
    }
}

// ── Event Card ──────────────────────────────────────────────────

@Composable
private fun EventCard(
    event: ScheduleEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeColor = getEventTypeColor(event.type)
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(typeColor)
            )
            Spacer(modifier = Modifier.width(14.dp))

            // Event info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (event.isAllDay) "All Day"
                        else "${timeFormatter.format(Date(event.startDatetime))} – ${timeFormatter.format(Date(event.endDatetime))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (!event.location.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = event.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Type badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getEventTypeIcon(event.type),
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Empty State ─────────────────────────────────────────────────

@Composable
private fun EmptyAgendaCard() {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.EventAvailable,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No events scheduled",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap + to create one",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────

private fun isSameDay(millis1: Long, millis2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun getEventTypeColor(type: String): Color = when (type.lowercase()) {
    "meeting" -> MeetingColor
    "appointment" -> Color(0xFF80CBC4)
    "deadline" -> DeadlineColor
    else -> AccentPrimary
}

private fun getEventTypeIcon(type: String): ImageVector = when (type.lowercase()) {
    "meeting" -> Icons.Outlined.Groups
    "appointment" -> Icons.Outlined.PersonOutline
    "deadline" -> Icons.Outlined.Timer
    else -> Icons.Outlined.Event
}

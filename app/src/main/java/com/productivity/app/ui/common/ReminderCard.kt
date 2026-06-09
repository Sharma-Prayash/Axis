package com.productivity.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.productivity.app.data.model.Reminder
import com.productivity.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Compact reminder card for dashboard display.
 * Shows the type icon, title, formatted time, and a priority dot.
 *
 * @param reminder  The [Reminder] to display.
 * @param onClick   Callback when the card is tapped.
 */
@Composable
fun DashboardReminderCard(
    reminder: Reminder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeColor = getReminderTypeColor(reminder.type)
    val priorityColor = getReminderPriorityColor(reminder.priority)
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
            // Type icon badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getReminderTypeIcon(reminder.type),
                    contentDescription = reminder.type,
                    tint = typeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title + time
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeFormatter.format(Date(reminder.datetime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    if (reminder.isSnoozed) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Snoozed",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarningAmber,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Priority dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(priorityColor)
            )
        }
    }
}

// ── Type / Priority Helpers ─────────────────────────────────────────

fun getReminderTypeColor(type: String): Color = when (type.lowercase()) {
    "medicine" -> MedicineColor
    "meeting" -> MeetingColor
    "travel" -> TravelColor
    "deadline" -> DeadlineColor
    else -> GeneralColor
}

fun getReminderTypeIcon(type: String): ImageVector = when (type.lowercase()) {
    "medicine" -> Icons.Outlined.Medication
    "meeting" -> Icons.Outlined.Groups
    "travel" -> Icons.Outlined.Flight
    "deadline" -> Icons.Outlined.Timer
    else -> Icons.Outlined.NotificationsNone
}

fun getReminderPriorityColor(priority: String): Color = when (priority.lowercase()) {
    "high" -> PriorityHigh
    "medium" -> PriorityMedium
    "low" -> PriorityLow
    else -> PriorityMedium
}

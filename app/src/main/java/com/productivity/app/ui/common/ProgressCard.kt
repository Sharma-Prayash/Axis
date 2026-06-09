package com.productivity.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.productivity.app.data.model.ProgressTracker
import com.productivity.app.ui.theme.*

/**
 * Compact progress tracker card for dashboard display.
 * Shows type icon, title, animated progress bar, and completion percentage.
 *
 * @param tracker  The [ProgressTracker] to display.
 * @param onClick  Callback when the card is tapped.
 */
@Composable
fun DashboardProgressCard(
    tracker: ProgressTracker,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeColor = if (tracker.type == "course") InfoBlue else AccentTertiary
    val progress = if (tracker.totalUnits > 0) {
        tracker.completedUnits.toFloat() / tracker.totalUnits.toFloat()
    } else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "dashboardTrackerProgress"
    )
    val percentText = "${(progress * 100).toInt()}%"
    val isComplete = tracker.completedAt != null

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top: type icon + title + percentage
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(typeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (tracker.type == "course") Icons.Outlined.School
                        else Icons.Outlined.WorkOutline,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tracker.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (tracker.currentUnitLabel != null) {
                        Text(
                            text = tracker.currentUnitLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = percentText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isComplete) SuccessGreen else typeColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isComplete) SuccessGreen else typeColor,
                trackColor = DarkSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Unit counts
            Text(
                text = "${tracker.completedUnits}/${tracker.totalUnits} units",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }
    }
}

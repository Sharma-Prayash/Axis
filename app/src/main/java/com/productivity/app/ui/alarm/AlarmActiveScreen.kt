package com.productivity.app.ui.alarm

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productivity.app.service.AlarmManagerHelper
import com.productivity.app.service.DoneReceiver
import com.productivity.app.service.NotificationHelper
import com.productivity.app.service.SnoozeReceiver
import com.productivity.app.ui.theme.*

@Composable
fun AlarmActiveScreen(
    reminderId: Long,
    eventId: Long,
    viewModel: AlarmActiveViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val title by viewModel.title.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()

    LaunchedEffect(reminderId, eventId) {
        viewModel.loadAlarmDetails(reminderId, eventId)
    }

    // Pulsing animation for the background/icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // ── Pulsing Icon ──
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                // Pulsing outer ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(AccentPrimary.copy(alpha = pulseAlpha))
                )
                // Middle ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(AccentPrimary.copy(alpha = 0.15f))
                )
                // Inner circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(AccentPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Alarm,
                        contentDescription = "Alarm Icon",
                        tint = DarkBackground,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Title & Info ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ALARM RINGING",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary,
                    letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing * 2
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title.ifBlank { "Ongoing Task Alert" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                if (type.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── Snooze & Stop Buttons ──
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
            ) {
                // Snooze is only available for reminders
                if (reminderId != -1L) {
                    Button(
                        onClick = {
                            val intent = Intent(context, SnoozeReceiver::class.java).apply {
                                putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
                            }
                            context.sendBroadcast(intent)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurface,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Text(
                            text = "Snooze (10 mins)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Button(
                    onClick = {
                        val intent = Intent(context, DoneReceiver::class.java).apply {
                            if (reminderId != -1L) putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
                            if (eventId != -1L) putExtra(AlarmManagerHelper.EXTRA_EVENT_ID, eventId)
                        }
                        context.sendBroadcast(intent)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Text(
                        text = "Stop Alarm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

package com.productivity.app.ui.reminders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.productivity.app.data.model.Reminder
import com.productivity.app.domain.reminder.Recurrence
import com.productivity.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailScreen(
    reminderId: Long,
    viewModel: ReminderViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val reminder by viewModel.selectedReminder.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSnoozeOptions by remember { mutableStateOf(false) }

    // Load the reminder on first composition, and reload whenever the screen
    // resumes (e.g. returning from the edit screen) so edits show immediately.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, reminderId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadReminder(reminderId)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ReminderUiEvent.ReminderCompleted -> {
                    snackbarHostState.showSnackbar("Reminder completed ✓")
                    onNavigateBack()
                }
                is ReminderUiEvent.ReminderDeleted -> {
                    onNavigateBack()
                }
                is ReminderUiEvent.ReminderSnoozed -> {
                    snackbarHostState.showSnackbar("Snoozed for ${event.durationMinutes} minutes")
                }
                is ReminderUiEvent.ReminderUpdated -> {
                    viewModel.loadReminder(reminderId)
                }
                is ReminderUiEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Reminder Details",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearSelection()
                        onNavigateBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (reminder?.isCompleted == false) {
                        IconButton(onClick = { onNavigateToEdit(reminderId) }) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Edit",
                                tint = AccentPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentPrimary)
            }
        } else if (reminder == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Reminder not found", color = TextSecondary)
            }
        } else {
            val rem = reminder!!
            val typeColor = getDetailTypeColor(rem.type)
            val priorityColor = getDetailPriorityColor(rem.priority)
            val dateFormatter = remember { SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()) }
            val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // ── Type Badge ───────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(typeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getDetailTypeIcon(rem.type),
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = rem.type.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge,
                            color = typeColor
                        )
                        Text(
                            text = if (rem.isCompleted) "Completed" else "Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (rem.isCompleted) SuccessGreen else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Title ────────────────────────────────────────
                Text(
                    text = rem.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Info Cards ───────────────────────────────────
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        DetailRow(
                            icon = Icons.Outlined.CalendarToday,
                            label = "Date",
                            value = dateFormatter.format(Date(rem.datetime)),
                            iconTint = AccentPrimary
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = DarkSurfaceVariant
                        )
                        DetailRow(
                            icon = Icons.Outlined.AccessTime,
                            label = "Time",
                            value = timeFormatter.format(Date(rem.datetime)),
                            iconTint = AccentPrimary
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = DarkSurfaceVariant
                        )
                        DetailRow(
                            icon = Icons.Outlined.Flag,
                            label = "Priority",
                            value = rem.priority.replaceFirstChar { it.uppercase() },
                            iconTint = priorityColor,
                            valueColor = priorityColor
                        )

                        if (rem.isSnoozed && rem.snoozeUntil != null) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = DarkSurfaceVariant
                            )
                            DetailRow(
                                icon = Icons.Outlined.Snooze,
                                label = "Snoozed Until",
                                value = "${dateFormatter.format(Date(rem.snoozeUntil))} at ${timeFormatter.format(Date(rem.snoozeUntil))}",
                                iconTint = WarningAmber,
                                valueColor = WarningAmber
                            )
                        }

                        if (Recurrence.isRecurring(rem.recurrenceRule)) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = DarkSurfaceVariant
                            )
                            DetailRow(
                                icon = Icons.Outlined.Repeat,
                                label = "Repeats",
                                value = Recurrence.label(rem.recurrenceRule),
                                iconTint = AccentTertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ── Action Buttons ───────────────────────────────
                if (!rem.isCompleted) {
                    // Complete Button
                    Button(
                        onClick = { viewModel.completeReminder(rem.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen,
                            contentColor = DarkBackground
                        )
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Mark as Done",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Snooze Button
                    OutlinedButton(
                        onClick = { showSnoozeOptions = !showSnoozeOptions },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WarningAmber
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = WarningAmber.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            Icons.Outlined.Snooze,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Snooze",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Snooze options
                    if (showSnoozeOptions) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(5, 10, 15, 30).forEach { minutes ->
                                OutlinedButton(
                                    onClick = {
                                        viewModel.snoozeReminder(rem.id, minutes)
                                        showSnoozeOptions = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 4.dp,
                                        vertical = 8.dp
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = WarningAmber
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = WarningAmber.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Text(
                                        "${minutes}m",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Delete Button
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ErrorRed
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = ErrorRed.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Delete Reminder",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ── Delete Confirmation Dialog ───────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    "Delete Reminder?",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    "This action cannot be undone. The reminder and its alarm will be permanently removed.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    reminder?.let { viewModel.deleteReminder(it.id) }
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color = AccentPrimary,
    valueColor: Color = TextPrimary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Helper Functions ────────────────────────────────────────────

private fun getDetailTypeColor(type: String): Color = when (type.lowercase()) {
    "medicine" -> MedicineColor
    "meeting" -> MeetingColor
    "travel" -> TravelColor
    "deadline" -> DeadlineColor
    else -> GeneralColor
}

private fun getDetailTypeIcon(type: String): ImageVector = when (type.lowercase()) {
    "medicine" -> Icons.Outlined.Medication
    "meeting" -> Icons.Outlined.Groups
    "travel" -> Icons.Outlined.Flight
    "deadline" -> Icons.Outlined.Timer
    else -> Icons.Outlined.NotificationsNone
}

private fun getDetailPriorityColor(priority: String): Color = when (priority.lowercase()) {
    "high" -> PriorityHigh
    "medium" -> PriorityMedium
    "low" -> PriorityLow
    else -> PriorityMedium
}

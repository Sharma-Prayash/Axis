package com.productivity.app.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.productivity.app.data.model.ScheduleEvent
import com.productivity.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Long,
    viewModel: ScheduleViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val event by viewModel.selectedEvent.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Load the event on first composition
    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { uiEvent ->
            when (uiEvent) {
                is ScheduleUiEvent.EventDeleted -> onNavigateBack()
                is ScheduleUiEvent.Error -> snackbarHostState.showSnackbar(uiEvent.message)
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Details", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearSelection()
                        onNavigateBack()
                    }) {
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
        } else if (event == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Event not found", color = TextSecondary)
            }
        } else {
            val evt = event!!
            val typeColor = getDetailTypeColor(evt.type)
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

                // ── Type Badge ───────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(typeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getDetailTypeIcon(evt.type),
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = evt.type.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge,
                            color = typeColor
                        )
                        Text(
                            text = if (evt.isAllDay) "All Day Event" else "Scheduled",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Title ────────────────────────────────────
                Text(
                    text = evt.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Info Card ────────────────────────────────
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        DetailInfoRow(
                            icon = Icons.Outlined.CalendarToday,
                            label = "Date",
                            value = dateFormatter.format(Date(evt.startDatetime)),
                            iconTint = AccentPrimary
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = DarkSurfaceVariant
                        )
                        DetailInfoRow(
                            icon = Icons.Outlined.AccessTime,
                            label = "Time",
                            value = if (evt.isAllDay) "All Day"
                            else "${timeFormatter.format(Date(evt.startDatetime))} – ${timeFormatter.format(Date(evt.endDatetime))}",
                            iconTint = AccentPrimary
                        )

                        if (!evt.location.isNullOrBlank()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = DarkSurfaceVariant
                            )
                            DetailInfoRow(
                                icon = Icons.Outlined.LocationOn,
                                label = "Location",
                                value = evt.location,
                                iconTint = SuccessGreen
                            )
                        }

                        if (!evt.notes.isNullOrBlank()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = DarkSurfaceVariant
                            )
                            DetailInfoRow(
                                icon = Icons.Outlined.Notes,
                                label = "Notes",
                                value = evt.notes,
                                iconTint = WarningAmber
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ── Delete Button ────────────────────────────
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
                    Text("Delete Event", fontWeight = FontWeight.SemiBold)
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
                Text("Delete Event?", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    "This action cannot be undone. The event will be permanently removed.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    event?.let { viewModel.deleteEvent(it.id) }
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

// ── Detail Info Row ─────────────────────────────────────────────

@Composable
private fun DetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color = AccentPrimary,
    valueColor: Color = TextPrimary
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
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
    "meeting" -> MeetingColor
    "appointment" -> Color(0xFF80CBC4)
    "deadline" -> DeadlineColor
    else -> AccentPrimary
}

private fun getDetailTypeIcon(type: String): ImageVector = when (type.lowercase()) {
    "meeting" -> Icons.Outlined.Groups
    "appointment" -> Icons.Outlined.PersonOutline
    "deadline" -> Icons.Outlined.Timer
    else -> Icons.Outlined.Event
}

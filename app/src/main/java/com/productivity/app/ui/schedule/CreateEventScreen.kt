package com.productivity.app.ui.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productivity.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEventScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
    eventId: Long = -1L,
    onNavigateBack: () -> Unit = {}
) {
    val isEdit = eventId != -1L

    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("event") }
    var customType by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isAllDay by remember { mutableStateOf(false) }

    val knownEventTypeIds = remember { setOf("event", "meeting", "appointment", "deadline", "custom") }

    // Date & time state
    val calendar = remember { Calendar.getInstance() }
    var selectedDateMillis by remember { mutableStateOf(calendar.timeInMillis) }
    var startHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }
    var endHour by remember { mutableIntStateOf((calendar.get(Calendar.HOUR_OF_DAY) + 1) % 24) }
    var endMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }

    // Dialog states
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val eventTypes = listOf("event", "meeting", "appointment", "deadline", "custom")
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // In edit mode, load the event and prefill the form once.
    val existing by viewModel.selectedEvent.collectAsStateWithLifecycle()
    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(eventId) {
        if (isEdit) viewModel.loadEvent(eventId)
    }
    LaunchedEffect(existing) {
        val evt = existing
        if (isEdit && !prefilled && evt != null && evt.id == eventId) {
            title = evt.title
            if (evt.type in knownEventTypeIds) {
                selectedType = evt.type
            } else {
                selectedType = "custom"
                customType = evt.type
            }
            location = evt.location ?: ""
            notes = evt.notes ?: ""
            isAllDay = evt.isAllDay
            selectedDateMillis = evt.startDatetime
            Calendar.getInstance().apply {
                timeInMillis = evt.startDatetime
                startHour = get(Calendar.HOUR_OF_DAY)
                startMinute = get(Calendar.MINUTE)
            }
            Calendar.getInstance().apply {
                timeInMillis = evt.endDatetime
                endHour = get(Calendar.HOUR_OF_DAY)
                endMinute = get(Calendar.MINUTE)
            }
            prefilled = true
        }
    }

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ScheduleUiEvent.EventCreated -> onNavigateBack()
                is ScheduleUiEvent.EventUpdated -> onNavigateBack()
                is ScheduleUiEvent.Error -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Event" else "New Event", fontWeight = FontWeight.SemiBold) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Title ────────────────────────────────────────
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Event Title") },
                placeholder = { Text("e.g. Team Standup") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentPrimary,
                    unfocusedLabelColor = TextTertiary,
                    cursorColor = AccentPrimary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Type Selector ────────────────────────────────
            Text(
                text = "Type",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                eventTypes.forEach { type ->
                    val isSelected = selectedType == type
                    val typeColor = getCreateEventTypeColor(type)
                    FilterChip(
                        onClick = { selectedType = type },
                        selected = isSelected,
                        label = {
                            Text(
                                type.replaceFirstChar { it.uppercase() },
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = typeColor.copy(alpha = 0.2f),
                            selectedLabelColor = typeColor,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = typeColor.copy(alpha = 0.5f),
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            if (selectedType == "custom") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = customType,
                    onValueChange = { customType = it },
                    label = { Text("Custom type") },
                    placeholder = { Text("e.g. Interview, Gym, Call") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = AccentPrimary,
                        unfocusedLabelColor = TextTertiary,
                        cursorColor = AccentPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── All Day Toggle ───────────────────────────────
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.WbSunny,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "All Day",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBackground,
                            checkedTrackColor = AccentPrimary,
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Date Picker ──────────────────────────────────
            Text(
                text = "Date",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = dateFormatter.format(Date(selectedDateMillis)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                }
            }

            // ── Time Pickers ─────────────────────────────────
            AnimatedVisibility(visible = !isAllDay) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Start time
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Start Time", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showStartTimePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.AccessTime,
                                        contentDescription = null,
                                        tint = AccentPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatTime(startHour, startMinute, timeFormatter),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                        // End time
                        Column(modifier = Modifier.weight(1f)) {
                            Text("End Time", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showEndTimePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.AccessTime,
                                        contentDescription = null,
                                        tint = WarningAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatTime(endHour, endMinute, timeFormatter),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Location ─────────────────────────────────────
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (optional)") },
                placeholder = { Text("e.g. Conference Room B") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextTertiary)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentPrimary,
                    unfocusedLabelColor = TextTertiary,
                    cursorColor = AccentPrimary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Notes ────────────────────────────────────────
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("Additional details...") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentPrimary,
                    unfocusedLabelColor = TextTertiary,
                    cursorColor = AccentPrimary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Create Button ────────────────────────────────
            Button(
                onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                    cal.set(Calendar.HOUR_OF_DAY, startHour)
                    cal.set(Calendar.MINUTE, startMinute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val startMillis = cal.timeInMillis

                    cal.set(Calendar.HOUR_OF_DAY, endHour)
                    cal.set(Calendar.MINUTE, endMinute)
                    val endMillis = cal.timeInMillis
                    val resolvedEnd = if (isAllDay) startMillis + 86_400_000 else endMillis
                    val resolvedType = if (selectedType == "custom") {
                        customType.trim().ifBlank { "custom" }
                    } else selectedType

                    if (isEdit) {
                        viewModel.updateEvent(
                            eventId = eventId,
                            title = title,
                            type = resolvedType,
                            startDatetime = startMillis,
                            endDatetime = resolvedEnd,
                            location = location,
                            notes = notes,
                            isAllDay = isAllDay
                        )
                    } else {
                        viewModel.createEvent(
                            title = title,
                            type = resolvedType,
                            startDatetime = startMillis,
                            endDatetime = resolvedEnd,
                            location = location,
                            notes = notes,
                            isAllDay = isAllDay
                        )
                    }
                },
                enabled = title.isNotBlank() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary,
                    contentColor = DarkBackground,
                    disabledContainerColor = AccentPrimary.copy(alpha = 0.3f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEdit) "Save Changes" else "Create Event", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── Date Picker Dialog ───────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) {
                    Text("OK", color = AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = DarkSurface)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    selectedDayContainerColor = AccentPrimary,
                    selectedDayContentColor = DarkBackground,
                    todayDateBorderColor = AccentPrimary,
                    todayContentColor = AccentPrimary
                )
            )
        }
    }

    // ── Start Time Picker Dialog ─────────────────────────────────
    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startHour,
            initialMinute = startMinute
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Start Time", color = TextPrimary) },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = DarkSurfaceVariant,
                        selectorColor = AccentPrimary,
                        containerColor = DarkSurface,
                        clockDialSelectedContentColor = DarkBackground,
                        clockDialUnselectedContentColor = TextPrimary,
                        timeSelectorSelectedContainerColor = AccentPrimary.copy(alpha = 0.2f),
                        timeSelectorSelectedContentColor = AccentPrimary,
                        timeSelectorUnselectedContainerColor = DarkSurfaceVariant,
                        timeSelectorUnselectedContentColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    startHour = timePickerState.hour
                    startMinute = timePickerState.minute
                    showStartTimePicker = false
                }) {
                    Text("OK", color = AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // ── End Time Picker Dialog ───────────────────────────────────
    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = endHour,
            initialMinute = endMinute
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("End Time", color = TextPrimary) },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = DarkSurfaceVariant,
                        selectorColor = AccentPrimary,
                        containerColor = DarkSurface,
                        clockDialSelectedContentColor = DarkBackground,
                        clockDialUnselectedContentColor = TextPrimary,
                        timeSelectorSelectedContainerColor = AccentPrimary.copy(alpha = 0.2f),
                        timeSelectorSelectedContentColor = AccentPrimary,
                        timeSelectorUnselectedContainerColor = DarkSurfaceVariant,
                        timeSelectorUnselectedContentColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    endHour = timePickerState.hour
                    endMinute = timePickerState.minute
                    showEndTimePicker = false
                }) {
                    Text("OK", color = AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

// ── Helpers ─────────────────────────────────────────────────────

private fun formatTime(hour: Int, minute: Int, formatter: SimpleDateFormat): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return formatter.format(cal.time)
}

private fun getCreateEventTypeColor(type: String): Color = when (type.lowercase()) {
    "meeting" -> MeetingColor
    "appointment" -> Color(0xFF80CBC4)
    "deadline" -> DeadlineColor
    else -> AccentPrimary
}

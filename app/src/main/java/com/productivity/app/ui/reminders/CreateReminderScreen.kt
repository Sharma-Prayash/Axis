package com.productivity.app.ui.reminders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.productivity.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateReminderScreen(
    viewModel: ReminderViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("general") }
    var selectedPriority by remember { mutableStateOf("medium") }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedHour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }

    // Handle successful creation
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            if (event is ReminderUiEvent.ReminderCreated) {
                onNavigateBack()
            }
        }
    }

    // Combine date and time into epoch millis
    val combinedDateTime = remember(selectedDateMillis, selectedHour, selectedMinute) {
        Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val reminderTypes = listOf(
        ReminderType("medicine", "Medicine", Icons.Outlined.Medication, MedicineColor),
        ReminderType("meeting", "Meeting", Icons.Outlined.Groups, MeetingColor),
        ReminderType("travel", "Travel", Icons.Outlined.Flight, TravelColor),
        ReminderType("deadline", "Deadline", Icons.Outlined.Timer, DeadlineColor),
        ReminderType("general", "General", Icons.Outlined.NotificationsNone, GeneralColor),
        ReminderType("custom", "Custom", Icons.Outlined.Edit, AccentTertiary)
    )

    val priorities = listOf(
        PriorityOption("low", "Low", PriorityLow),
        PriorityOption("medium", "Medium", PriorityMedium),
        PriorityOption("high", "High", PriorityHigh)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "New Reminder",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Title ────────────────────────────────────────────
            Text(
                text = "Title",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("What do you need to remember?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    cursorColor = AccentPrimary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedPlaceholderColor = TextTertiary,
                    unfocusedPlaceholderColor = TextTertiary,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Type Selector ────────────────────────────────────
            Text(
                text = "Type",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reminderTypes.forEach { type ->
                    val isSelected = selectedType == type.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type.id },
                        label = { Text(type.label) },
                        leadingIcon = {
                            Icon(
                                type.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = type.color.copy(alpha = 0.2f),
                            selectedLabelColor = type.color,
                            selectedLeadingIconColor = type.color,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary,
                            iconColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = DarkSurfaceVariant,
                            selectedBorderColor = type.color.copy(alpha = 0.5f),
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Date & Time ──────────────────────────────────────
            Text(
                text = "When",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Date picker trigger
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
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
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = dateFormatter.format(Date(selectedDateMillis)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }

                // Time picker trigger
                Card(
                    modifier = Modifier
                        .weight(0.6f)
                        .clickable { showTimePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
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
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = timeFormatter.format(
                                Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, selectedHour)
                                    set(Calendar.MINUTE, selectedMinute)
                                }.time
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Priority ─────────────────────────────────────────
            Text(
                text = "Priority",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                priorities.forEach { priority ->
                    val isSelected = selectedPriority == priority.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPriority = priority.id },
                        label = { Text(priority.label) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = priority.color.copy(alpha = 0.2f),
                            selectedLabelColor = priority.color,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = DarkSurfaceVariant,
                            selectedBorderColor = priority.color.copy(alpha = 0.5f),
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── Create Button ────────────────────────────────────
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.createReminder(
                            title = title.trim(),
                            type = selectedType,
                            datetime = combinedDateTime,
                            priority = selectedPriority
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary,
                    contentColor = DarkBackground,
                    disabledContainerColor = AccentPrimary.copy(alpha = 0.3f),
                    disabledContentColor = DarkBackground.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    "Create Reminder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
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
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMillis = it
                    }
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
            colors = DatePickerDefaults.colors(
                containerColor = DarkSurface
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    navigationContentColor = TextPrimary,
                    yearContentColor = TextSecondary,
                    currentYearContentColor = AccentPrimary,
                    selectedYearContainerColor = AccentPrimary,
                    selectedDayContainerColor = AccentPrimary,
                    todayContentColor = AccentPrimary,
                    todayDateBorderColor = AccentPrimary,
                    dayContentColor = TextPrimary
                )
            )
        }
    }

    // ── Time Picker Dialog ───────────────────────────────────────
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text(
                    "Select Time",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            },
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
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) {
                    Text("OK", color = AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

// ── Data classes ─────────────────────────────────────────────────

private data class ReminderType(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color
)

private data class PriorityOption(
    val id: String,
    val label: String,
    val color: androidx.compose.ui.graphics.Color
)

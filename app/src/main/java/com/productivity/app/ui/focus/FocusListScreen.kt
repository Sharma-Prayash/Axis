package com.productivity.app.ui.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productivity.app.data.model.FocusTask
import com.productivity.app.ui.theme.*
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusListScreen(
    viewModel: FocusViewModel = hiltViewModel(),
    onNavigateToTimer: (Long) -> Unit
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val todayLogs by viewModel.todayLogs.collectAsStateWithLifecycle()
    var showCreateSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = AccentPrimary,
                contentColor = DarkBackground
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Focus Goal")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // ── Header ───────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Dedicated Focus Goals",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Create custom Pomodoro cycles to power through learning and log daily target completion.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Tasks list ───────────────────────────────────
            if (tasks.isEmpty()) {
                item { EmptyTasksCard() }
            } else {
                items(tasks, key = { it.id }) { task ->
                    val todayMinutes = todayLogs[task.id] ?: 0
                    FocusTaskCard(
                        task = task,
                        todayMinutes = todayMinutes,
                        onClick = { onNavigateToTimer(task.id) },
                        onDelete = { viewModel.deleteTask(task) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (showCreateSheet) {
            CreateFocusTaskBottomSheet(
                onDismiss = { showCreateSheet = false },
                onCreate = { title, desc, targetMin, workMin, breakMin, scaling, increment ->
                    viewModel.createTask(title, desc, targetMin, workMin, breakMin, scaling, increment)
                    showCreateSheet = false
                }
            )
        }
    }
}

@Composable
private fun EmptyTasksCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No focus goals created yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Create a goal for study, project work, or any task to build high-consistency streaks.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun FocusTaskCard(
    task: FocusTask,
    todayMinutes: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (task.dailyTargetMinutes > 0) {
        todayMinutes.toFloat() / task.dailyTargetMinutes.toFloat()
    } else 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "focusProgress"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Task Details Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HourglassEmpty,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!task.description.isNullOrBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress text
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Today: $todayMinutes / ${task.dailyTargetMinutes} mins",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "${(progress * 100).toInt()}% completed",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (progress >= 1f) SuccessGreen else AccentPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (progress >= 1f) SuccessGreen else AccentPrimary,
                trackColor = DarkSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Pomodoro config display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.PlayCircle,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${task.workDurationMinutes}m focus",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Coffee,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${task.breakDurationMinutes}m break",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
                if (task.enableGradualScaling) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(InfoBlue.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Gradual Scaling",
                            style = MaterialTheme.typography.labelSmall,
                            color = InfoBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateFocusTaskBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String?, targetMinutes: Int, workMinutes: Int, breakMinutes: Int, enableGradualScaling: Boolean, gradualIncrement: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    var targetHoursInput by remember { mutableStateOf("1") }
    var targetMinutesInput by remember { mutableStateOf("0") }

    var workMinutesInput by remember { mutableStateOf("25") }
    var breakMinutesInput by remember { mutableStateOf("5") }

    var enableGradualScaling by remember { mutableStateOf(false) }
    var gradualIncrementInput by remember { mutableStateOf("5") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextTertiary) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "New Focus Goal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable fields container
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Title
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title") },
                        placeholder = { Text("e.g. Kotlin Study, Writing Book") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedLabelColor = AccentPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Description
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedLabelColor = AccentPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Daily Target Time Division
                item {
                    Text(
                        text = "Daily Target Duration",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = targetHoursInput,
                            onValueChange = { targetHoursInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Hours") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                unfocusedTextColor = TextPrimary,
                                focusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = targetMinutesInput,
                            onValueChange = { targetMinutesInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Minutes") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                unfocusedTextColor = TextPrimary,
                                focusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Cycle presets
                item {
                    Text(
                        text = "Pomodoro Configuration Presets",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            Triple("25m / 5m", "25", "5"),
                            Triple("50m / 10m", "50", "10"),
                            Triple("90m / 15m", "90", "15")
                        ).forEach { (label, work, brk) ->
                            val isSelected = workMinutesInput == work && breakMinutesInput == brk
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                border = if (isSelected) BorderStroke(1.dp, AccentPrimary) else null,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) AccentPrimary.copy(alpha = 0.15f) else DarkSurfaceVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        workMinutesInput = work
                                        breakMinutesInput = brk
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) AccentPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom cycle durations
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = workMinutesInput,
                            onValueChange = { workMinutesInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Study Mins") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                unfocusedTextColor = TextPrimary,
                                focusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = breakMinutesInput,
                            onValueChange = { breakMinutesInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Break Mins") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                unfocusedTextColor = TextPrimary,
                                focusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Gradual cycle scaling option
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gradual Cycle Scaling",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Increase focus by +5m and break by +1m after each cycle to train mental endurance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = enableGradualScaling,
                            onCheckedChange = { enableGradualScaling = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentPrimary,
                                checkedTrackColor = AccentPrimary.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = DarkSurface
                            )
                        )
                    }
                }

                // Increments input if scaling enabled
                item {
                    AnimatedVisibility(visible = enableGradualScaling) {
                        OutlinedTextField(
                            value = gradualIncrementInput,
                            onValueChange = { gradualIncrementInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Minutes Increment Per Work Cycle") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                unfocusedTextColor = TextPrimary,
                                focusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Save button
                item {
                    val isValid = title.isNotBlank() && 
                            ((targetHoursInput.toIntOrNull() ?: 0) * 60 + (targetMinutesInput.toIntOrNull() ?: 0) > 0) &&
                            (workMinutesInput.toIntOrNull() ?: 0) > 0 && 
                            (breakMinutesInput.toIntOrNull() ?: 0) > 0

                    Button(
                        onClick = {
                            val targetMins = (targetHoursInput.toIntOrNull() ?: 0) * 60 + (targetMinutesInput.toIntOrNull() ?: 0)
                            val workMins = workMinutesInput.toIntOrNull() ?: 25
                            val breakMins = breakMinutesInput.toIntOrNull() ?: 5
                            val gradualIncrement = gradualIncrementInput.toIntOrNull() ?: 5

                            onCreate(
                                title,
                                description.takeIf { it.isNotBlank() },
                                targetMins,
                                workMins,
                                breakMins,
                                enableGradualScaling,
                                gradualIncrement
                            )
                        },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = DarkBackground,
                            disabledContainerColor = DarkSurfaceVariant,
                            disabledContentColor = TextTertiary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Create Goal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

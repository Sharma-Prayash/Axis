package com.productivity.app.ui.tracker

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productivity.app.data.model.ProgressUnit
import com.productivity.app.data.model.WeeklyGoal
import com.productivity.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerDetailScreen(
    trackerId: Long,
    viewModel: TrackerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val tracker by viewModel.selectedTracker.collectAsStateWithLifecycle()
    val units by viewModel.selectedTrackerUnits.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val weeklyGoal by viewModel.weeklyGoal.collectAsStateWithLifecycle()
    val completionDates by viewModel.completionDatesMap.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddUnit by remember { mutableStateOf(false) }
    var newUnitTitle by remember { mutableStateOf("") }

    // Load the tracker on first composition
    LaunchedEffect(trackerId) {
        viewModel.loadTracker(trackerId)
    }

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is TrackerUiEvent.TrackerDeleted -> onNavigateBack()
                is TrackerUiEvent.TrackerCompleted -> {
                    snackbarHostState.showSnackbar("🎉 Tracker completed!")
                }
                is TrackerUiEvent.UnitAdded -> {
                    newUnitTitle = ""
                    snackbarHostState.showSnackbar("Unit added")
                }
                is TrackerUiEvent.UnitCompleted -> {
                    snackbarHostState.showSnackbar("Unit completed ✓")
                }
                is TrackerUiEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tracker Details", fontWeight = FontWeight.SemiBold) },
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
        } else if (tracker == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Tracker not found", color = TextSecondary)
            }
        } else {
            val trk = tracker!!
            val typeColor = if (trk.type == "course") InfoBlue else AccentTertiary
            val progress = if (trk.totalUnits > 0) {
                trk.completedUnits.toFloat() / trk.totalUnits.toFloat()
            } else 0f
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 600),
                label = "detailProgress"
            )
            val isComplete = trk.completedAt != null

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // ── Header ───────────────────────────────────
                item {
                    // Type badge + Title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(typeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (trk.type == "course") Icons.Outlined.School
                                else Icons.Outlined.WorkOutline,
                                contentDescription = null,
                                tint = typeColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = trk.type.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelLarge,
                                color = typeColor
                            )
                            Text(
                                text = if (isComplete) "Completed" else "In Progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isComplete) SuccessGreen else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = trk.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (!trk.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = trk.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Progress Card ────────────────────────
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Progress",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isComplete) SuccessGreen else typeColor
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (isComplete) SuccessGreen else typeColor,
                                trackColor = DarkSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row {
                                Text(
                                    text = "${trk.completedUnits} of ${trk.totalUnits} ${if (trk.type == "course") "modules" else "milestones"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    WeeklyGoalCard(
                        weeklyGoal = weeklyGoal,
                        onSetGoal = { viewModel.setWeeklyGoal(it) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ConsistencyGrid(
                        completionDates = completionDates,
                        typeColor = typeColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Units Section Header ─────────────────
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (trk.type == "course") "Modules" else "Milestones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (!isComplete) {
                            TextButton(onClick = { showAddUnit = !showAddUnit }) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add")
                            }
                        }
                    }

                    // ── Inline Add Unit ──────────────────────
                    if (showAddUnit) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newUnitTitle,
                                    onValueChange = { newUnitTitle = it },
                                    placeholder = {
                                        Text(
                                            if (trk.type == "course") "Module name"
                                            else "Milestone name"
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = typeColor,
                                        unfocusedBorderColor = DarkSurfaceVariant,
                                        cursorColor = typeColor,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledIconButton(
                                    onClick = {
                                        if (newUnitTitle.isNotBlank()) {
                                            viewModel.addUnit(newUnitTitle)
                                        }
                                    },
                                    enabled = newUnitTitle.isNotBlank(),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = typeColor,
                                        contentColor = DarkBackground
                                    )
                                ) {
                                    Icon(
                                        Icons.Outlined.Add,
                                        contentDescription = "Add",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Unit List ────────────────────────────────
                if (units.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ListAlt,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No ${if (trk.type == "course") "modules" else "milestones"} yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap \"+ Add\" to create one",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(units, key = { _, unit -> unit.id }) { index, unit ->
                        UnitItem(
                            unit = unit,
                            index = index + 1,
                            typeColor = typeColor,
                            trackerType = trk.type,
                            onComplete = { viewModel.completeUnit(unit.id) }
                        )
                        if (index < units.lastIndex) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                // ── Delete Button ────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(28.dp))

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
                        Text("Delete Tracker", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // ── Delete Confirmation Dialog ───────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("Delete Tracker?", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    "This will permanently delete the tracker and all its ${if (tracker?.type == "course") "modules" else "milestones"}.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    tracker?.let { viewModel.deleteTracker(it.id) }
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

// ── Unit Item ───────────────────────────────────────────────────

@Composable
private fun UnitItem(
    unit: ProgressUnit,
    index: Int,
    typeColor: Color,
    trackerType: String,
    onComplete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Order index
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (unit.isCompleted) SuccessGreen.copy(alpha = 0.15f)
                        else DarkSurfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (unit.isCompleted) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Unit info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = unit.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (unit.isCompleted) TextTertiary else TextPrimary,
                    textDecoration = if (unit.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (unit.isCompleted && unit.completedAt != null) {
                    Text(
                        text = "Completed ${dateFormatter.format(Date(unit.completedAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            // Complete button
            if (!unit.isCompleted) {
                Checkbox(
                    checked = false,
                    onCheckedChange = { onComplete() },
                    colors = CheckboxDefaults.colors(
                        uncheckedColor = typeColor,
                        checkedColor = SuccessGreen,
                        checkmarkColor = DarkBackground
                    )
                )
            }
        }
    }
}

@Composable
fun ConsistencyGrid(
    completionDates: Map<java.time.LocalDate, Int>,
    typeColor: Color
) {
    val today = java.time.LocalDate.now()
    val completedDates = completionDates.keys
    val minDate = completedDates.minOrNull() ?: today
    
    // Find the Monday of the starting week
    val minDateWeekStart = minDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val todayWeekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    
    val weeksBetween = java.time.temporal.ChronoUnit.WEEKS.between(minDateWeekStart, todayWeekStart).toInt()
    val totalWeeksToShow = maxOf(12, weeksBetween + 1) // minimum of 12 weeks, grows dynamically!
    
    val startOfWeek = todayWeekStart.minusWeeks((totalWeeksToShow - 1).toLong())
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Consistency History",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Grid container (horizontal scrollable)
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(totalWeeksToShow) { weekIndex ->
                    val weekMonday = startOfWeek.plusWeeks(weekIndex.toLong())
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (dayOffset in 0..6) {
                            val date = weekMonday.plusDays(dayOffset.toLong())
                            val completedCount = completionDates[date] ?: 0
                            val cellColor = when {
                                date.isAfter(today) -> DarkSurfaceVariant.copy(alpha = 0.2f) // Future date
                                completedCount == 0 -> DarkSurfaceVariant // No completion
                                completedCount == 1 -> typeColor.copy(alpha = 0.4f)
                                completedCount == 2 -> typeColor.copy(alpha = 0.7f)
                                else -> typeColor // 3+ completions
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(cellColor)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Legend
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Less ", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(DarkSurfaceVariant))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(typeColor.copy(alpha = 0.4f)))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(typeColor.copy(alpha = 0.7f)))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(typeColor))
                Spacer(modifier = Modifier.width(4.dp))
                Text(" More", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
        }
    }
}

@Composable
fun WeeklyGoalCard(
    weeklyGoal: WeeklyGoal?,
    onSetGoal: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var targetInput by remember { mutableStateOf("") }
    
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
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Weekly Goal",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { 
                    targetInput = weeklyGoal?.targetCount?.toString() ?: ""
                    showDialog = true 
                }) {
                    Text(if (weeklyGoal == null) "Set Target" else "Edit Target")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            if (weeklyGoal != null) {
                val achieved = weeklyGoal.achievedCount
                val target = weeklyGoal.targetCount
                val goalProgress = if (target > 0) achieved.toFloat() / target.toFloat() else 0f
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Achieved $achieved of $target units this week",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
                    Spacer(modifier = Modifier.width(16.dp))
                    if (achieved >= target && target > 0) {
                        Text(
                            text = "🎯 Done!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    } else {
                        Text(
                            text = "${(goalProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentPrimary
                        )
                    }
                }
            } else {
                Text(
                    text = "No goal set for this week. Stay consistent by setting a completion target!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )
            }
        }
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Set Weekly Target", color = TextPrimary) },
            text = {
                Column {
                    Text("Enter the number of units you want to complete this week:", color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { targetInput = it },
                        placeholder = { Text("e.g. 5") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val count = targetInput.toIntOrNull()
                    if (count != null && count > 0) {
                        onSetGoal(count)
                        showDialog = false
                    }
                }) {
                    Text("Save", color = AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

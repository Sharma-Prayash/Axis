package com.productivity.app.ui.focus

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import com.productivity.app.data.model.FocusTask
import com.productivity.app.service.FocusTimerService
import com.productivity.app.service.TimerState
import com.productivity.app.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    taskId: Long,
    viewModel: FocusViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Bind to Foreground FocusTimerService
    var service by remember { mutableStateOf<FocusTimerService?>(null) }
    var isBound by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val localBinder = binder as? FocusTimerService.LocalBinder
                service = localBinder?.getService()
                isBound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
                isBound = false
            }
        }
    }

    DisposableEffect(taskId) {
        val intent = Intent(context, FocusTimerService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        onDispose {
            if (isBound) {
                context.unbindService(connection)
                isBound = false
            }
        }
    }

    // Load task metadata and history
    LaunchedEffect(taskId) {
        viewModel.selectTask(taskId)
    }

    val task by viewModel.selectedTask.collectAsStateWithLifecycle()
    val todayLogs by viewModel.todayLogs.collectAsStateWithLifecycle()
    val consistencyMap by viewModel.selectedTaskConsistencyMap.collectAsStateWithLifecycle()

    // Collect service states reactively and safely
    val activeTaskFlow =
        remember(service) { service?.currentTask ?: MutableStateFlow<FocusTask?>(null) }
    val activeTask by activeTaskFlow.collectAsStateWithLifecycle()

    val timerStateFlow =
        remember(service) { service?.timerState ?: MutableStateFlow(TimerState.IDLE) }
    val timerState by timerStateFlow.collectAsStateWithLifecycle()

    val secondsRemainingFlow =
        remember(service) { service?.secondsRemaining ?: MutableStateFlow(0) }
    val secondsRemaining by secondsRemainingFlow.collectAsStateWithLifecycle()

    val maxSecondsFlow = remember(service) { service?.maxSeconds ?: MutableStateFlow(0) }
    val maxSeconds by maxSecondsFlow.collectAsStateWithLifecycle()

    val completedCyclesFlow = remember(service) { service?.completedCycles ?: MutableStateFlow(0) }
    val completedCycles by completedCyclesFlow.collectAsStateWithLifecycle()

    val todayMinutes = task?.let { todayLogs[it.id] ?: 0 } ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(task?.title ?: "Focus Timer", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val currentTask = task
                    if (currentTask != null) {
                        IconButton(onClick = { showEditSheet = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit Goal",
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
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
        ) {
            val currentTaskLocal = task
            if (currentTaskLocal == null) {
                item {
                    CircularProgressIndicator(color = AccentPrimary)
                }
            } else {
                // If another timer is running
                val currentActiveTask = activeTask
                if (currentActiveTask != null && currentActiveTask.id != currentTaskLocal.id) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = WarningAmber,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Another Session Running",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "A focus session is currently active for \"${currentActiveTask.title}\". Close that session to start this one.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { service?.stopSession() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ErrorRed,
                                        contentColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Stop Running Session")
                                }
                            }
                        }
                    }
                } else {
                    // ── Active Timer Ring Section ───────────────────────
                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Phase state description
                        val phaseText = when (timerState) {
                            TimerState.WORK_TICKING -> "FOCUSING"
                            TimerState.WORK_PAUSED -> "FOCUS PAUSED"
                            TimerState.BREAK_TICKING -> "ON BREAK"
                            TimerState.BREAK_PAUSED -> "BREAK PAUSED"
                            else -> "READY"
                        }

                        val phaseColor =
                            if (timerState == TimerState.BREAK_TICKING || timerState == TimerState.BREAK_PAUSED) {
                                SuccessGreen
                            } else {
                                AccentPrimary
                            }

                        Text(
                            text = phaseText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = phaseColor,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Large Progress Circle
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(260.dp)
                        ) {
                            val progressValue = if (maxSeconds > 0) {
                                secondsRemaining.toFloat() / maxSeconds.toFloat()
                            } else 1f

                            val animatedProgress by animateFloatAsState(
                                targetValue = progressValue,
                                label = "ringProgress"
                            )

                            // Background circle
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.fillMaxSize(),
                                color = DarkSurfaceVariant,
                                strokeWidth = 12.dp
                            )

                            // Foreground ticking progress
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.fillMaxSize(),
                                color = phaseColor,
                                strokeWidth = 12.dp
                            )

                            // Remaining Time Text
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val displaySeconds = if (timerState == TimerState.IDLE) {
                                    currentTaskLocal.workDurationMinutes * 60
                                } else {
                                    secondsRemaining
                                }
                                Text(
                                    text = formatTime(displaySeconds),
                                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 44.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (timerState != TimerState.IDLE) {
                                    Text(
                                        text = "Cycle #${completedCycles + 1}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                } else {
                                    Text(
                                        text = "Target: ${currentTaskLocal.dailyTargetMinutes}m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // ── Timer Control Buttons ───────────────────────────
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (timerState) {
                                TimerState.IDLE -> {
                                    Button(
                                        onClick = {
                                            // Start Foreground Service first, then start session
                                            val serviceIntent =
                                                Intent(context, FocusTimerService::class.java)
                                            ContextCompat.startForegroundService(
                                                context,
                                                serviceIntent
                                            )
                                            service?.startSession(currentTaskLocal)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AccentPrimary,
                                            contentColor = DarkBackground
                                        ),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(52.dp)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Start Focus", fontWeight = FontWeight.Bold)
                                    }
                                }

                                TimerState.WORK_TICKING, TimerState.BREAK_TICKING -> {
                                    FilledIconButton(
                                        onClick = { service?.pauseSession() },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = DarkSurface,
                                            contentColor = TextPrimary
                                        ),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Pause,
                                            contentDescription = "Pause",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    FilledIconButton(
                                        onClick = { service?.stopSession() },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = ErrorRed.copy(alpha = 0.15f),
                                            contentColor = ErrorRed
                                        ),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Stop,
                                            contentDescription = "Stop",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                TimerState.WORK_PAUSED, TimerState.BREAK_PAUSED -> {
                                    FilledIconButton(
                                        onClick = { service?.resumeSession() },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = AccentPrimary,
                                            contentColor = DarkBackground
                                        ),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = "Resume",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    FilledIconButton(
                                        onClick = { service?.stopSession() },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = ErrorRed.copy(alpha = 0.15f),
                                            contentColor = ErrorRed
                                        ),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Stop,
                                            contentDescription = "Stop",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // ── Target & Settings Summary Card ────────────────────
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Session Settings",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Cycle Work Interval",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        "${currentTaskLocal.workDurationMinutes} minutes",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Cycle Break Interval",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        "${currentTaskLocal.breakDurationMinutes} minutes",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                if (currentTaskLocal.enableGradualScaling) {
                                    Divider(
                                        color = DarkSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Interval Scaling (+)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                        Text(
                                            "+${currentTaskLocal.gradualMinutesIncrement}m work / +1m break",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = InfoBlue
                                        )
                                    }

                                    if (timerState != TimerState.IDLE && completedCycles > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val workBonus =
                                            completedCycles * currentTaskLocal.gradualMinutesIncrement
                                        val breakBonus = completedCycles
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                "Current Scaling Bonus",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary
                                            )
                                            Text(
                                                "+$workBonus mins study / +$breakBonus mins break",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = SuccessGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // ── Today's Goal Progress Card ────────────────────────
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
                                        Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = if (todayMinutes >= currentTaskLocal.dailyTargetMinutes) SuccessGreen else AccentPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Today's Target Progress",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "$todayMinutes / ${currentTaskLocal.dailyTargetMinutes} mins",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                val progressPercent = if (currentTaskLocal.dailyTargetMinutes > 0) {
                                    todayMinutes.toFloat() / currentTaskLocal.dailyTargetMinutes.toFloat()
                                } else 0f
                                LinearProgressIndicator(
                                    progress = { progressPercent.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (todayMinutes >= currentTaskLocal.dailyTargetMinutes) SuccessGreen else AccentPrimary,
                                    trackColor = DarkSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ── GitHub Consistency Grid ───────────────────────────
                        FocusConsistencyGrid(
                            consistencyMap = consistencyMap,
                            targetMinutes = currentTaskLocal.dailyTargetMinutes,
                            gridColor = AccentPrimary
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }

        if (showEditSheet) {
            FocusTaskBottomSheet(
                taskToEdit = task,
                onDismiss = { showEditSheet = false },
                onSave = { title, desc, targetMin, workMin, breakMin, scaling, increment ->
                    val currentTaskVal = task
                    if (currentTaskVal != null) {
                        viewModel.updateTask(
                            id = currentTaskVal.id,
                            title = title,
                            description = desc,
                            dailyTargetMinutes = targetMin,
                            workMinutes = workMin,
                            breakMinutes = breakMin,
                            enableGradualScaling = scaling,
                            gradualIncrement = increment
                        )
                        val updatedTask = currentTaskVal.copy(
                            title = title,
                            description = desc,
                            dailyTargetMinutes = targetMin,
                            workDurationMinutes = workMin,
                            breakDurationMinutes = breakMin,
                            enableGradualScaling = scaling,
                            gradualMinutesIncrement = increment
                        )
                        service?.updateActiveTask(updatedTask)
                    }
                    showEditSheet = false
                }
            )
        }
    }
}

@Composable
private fun FocusConsistencyGrid(
    consistencyMap: Map<LocalDate, Int>,
    targetMinutes: Int,
    gridColor: Color
) {
    val today = LocalDate.now()
    val minDate = consistencyMap.keys.minOrNull() ?: today
    
    val minDateWeekStart = minDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val todayWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    
    val weeksBetween = ChronoUnit.WEEKS.between(minDateWeekStart, todayWeekStart).toInt()
    val totalWeeksToShow = maxOf(12, weeksBetween + 1)
    
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

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(totalWeeksToShow) { weekIndex ->
                    val weekMonday = startOfWeek.plusWeeks(weekIndex.toLong())
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (dayOffset in 0..6) {
                            val date = weekMonday.plusDays(dayOffset.toLong())
                            val completedMinutes = consistencyMap[date] ?: 0
                            
                            val cellColor = when {
                                date.isAfter(today) -> DarkSurfaceVariant.copy(alpha = 0.2f)
                                completedMinutes == 0 -> DarkSurfaceVariant
                                completedMinutes < targetMinutes / 3 -> gridColor.copy(alpha = 0.35f)
                                completedMinutes < targetMinutes * 2 / 3 -> gridColor.copy(alpha = 0.65f)
                                else -> SuccessGreen // Met or exceeded target!
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
                Text("Less ", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(DarkSurfaceVariant))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(gridColor.copy(alpha = 0.35f)))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(gridColor.copy(alpha = 0.65f)))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(SuccessGreen))
                Spacer(modifier = Modifier.width(4.dp))
                Text(" Target Met", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}
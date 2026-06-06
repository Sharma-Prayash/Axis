package com.productivity.app.ui.settings

import android.app.Activity
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productivity.app.data.preferences.AlarmPreferences
import com.productivity.app.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingsScreen(
    viewModel: AlarmSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val ringDuration by viewModel.ringDuration.collectAsStateWithLifecycle()
    val alarmToneUri by viewModel.alarmToneUri.collectAsStateWithLifecycle()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()

    var showDurationPicker by remember { mutableStateOf(false) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Media player for preview
    val previewPlayer = remember { mutableStateOf<MediaPlayer?>(null) }

    // Cleanup preview player on dispose
    DisposableEffect(Unit) {
        onDispose {
            previewPlayer.value?.let {
                try {
                    if (it.isPlaying) it.stop()
                    it.release()
                } catch (_: Exception) {}
            }
        }
    }

    // Auto-stop preview after 3 seconds
    LaunchedEffect(isPreviewPlaying) {
        if (isPreviewPlaying) {
            delay(3000)
            previewPlayer.value?.let {
                try {
                    if (it.isPlaying) it.stop()
                    it.reset()
                    it.release()
                } catch (_: Exception) {}
            }
            previewPlayer.value = null
            isPreviewPlaying = false
        }
    }

    // Ringtone picker launcher
    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            viewModel.updateAlarmToneUri(uri?.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Alarm Settings",
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

            // ── Section: Sound ──────────────────────────────────────
            SectionHeader(title = "Sound", icon = Icons.Outlined.VolumeUp)

            Spacer(modifier = Modifier.height(12.dp))

            // Alarm Tone Selector
            SettingsCard(
                title = "Alarm Tone",
                subtitle = viewModel.getAlarmToneName(),
                icon = Icons.Outlined.MusicNote,
                onClick = {
                    val currentUri = alarmToneUri?.let { Uri.parse(it) }
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

                    val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(
                            RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        )
                    }
                    ringtoneLauncher.launch(intent)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Preview Button
            SettingsCard(
                title = if (isPreviewPlaying) "Playing..." else "Preview Tone",
                subtitle = if (isPreviewPlaying) "Tap to stop" else "Tap to hear a 3-second preview",
                icon = if (isPreviewPlaying) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                iconTint = if (isPreviewPlaying) ErrorRed else AccentPrimary,
                onClick = {
                    if (isPreviewPlaying) {
                        // Stop preview
                        previewPlayer.value?.let {
                            try {
                                if (it.isPlaying) it.stop()
                                it.reset()
                                it.release()
                            } catch (_: Exception) {}
                        }
                        previewPlayer.value = null
                        isPreviewPlaying = false
                    } else {
                        // Start preview
                        try {
                            val toneUri = alarmToneUri?.let { Uri.parse(it) }
                                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

                            val player = MediaPlayer().apply {
                                setAudioAttributes(
                                    AudioAttributes.Builder()
                                        .setUsage(AudioAttributes.USAGE_ALARM)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                        .build()
                                )
                                setDataSource(context, toneUri)
                                isLooping = false
                                prepare()
                                start()
                            }
                            previewPlayer.value = player
                            isPreviewPlaying = true
                        } catch (_: Exception) {
                            isPreviewPlaying = false
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Section: Duration ───────────────────────────────────
            SectionHeader(title = "Duration", icon = Icons.Outlined.Timer)

            Spacer(modifier = Modifier.height(12.dp))

            // Ring Duration Selector
            SettingsCard(
                title = "Ring Duration",
                subtitle = AlarmPreferences.durationLabel(ringDuration),
                icon = Icons.Outlined.AccessTime,
                onClick = { showDurationPicker = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Section: Vibration ──────────────────────────────────
            SectionHeader(title = "Haptics", icon = Icons.Outlined.Vibration)

            Spacer(modifier = Modifier.height(12.dp))

            // Vibration Toggle
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Vibration,
                            contentDescription = null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Vibrate on Alarm",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            if (vibrationEnabled) "Vibration is on" else "Vibration is off",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { viewModel.updateVibration(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentPrimary,
                            checkedTrackColor = AccentPrimary.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Info Note ───────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AccentPrimary.copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "The alarm tone uses the alarm volume slider on your device. " +
                                "Changing the tone here does not affect your system's built-in alarm clock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── Duration Picker Dialog ──────────────────────────────────────
    if (showDurationPicker) {
        AlertDialog(
            onDismissRequest = { showDurationPicker = false },
            title = {
                Text(
                    "Ring Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    AlarmPreferences.DURATION_OPTIONS.forEach { seconds ->
                        val isSelected = ringDuration == seconds
                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) AccentPrimary.copy(alpha = 0.15f)
                            else DarkSurface,
                            label = "duration_bg"
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable {
                                    viewModel.updateRingDuration(seconds)
                                    showDurationPicker = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.updateRingDuration(seconds)
                                        showDurationPicker = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = AccentPrimary,
                                        unselectedColor = TextTertiary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = AlarmPreferences.durationLabel(seconds),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) AccentPrimary else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDurationPicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ── Reusable Components ─────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = AccentPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = AccentPrimary
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color = AccentPrimary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

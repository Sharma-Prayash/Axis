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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingsScreen(
    viewModel: AlarmSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val ringDuration by viewModel.ringDuration.collectAsStateWithLifecycle()
    val alarmToneUri by viewModel.alarmToneUri.collectAsStateWithLifecycle()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()

    val workspaceDomain by viewModel.workspaceDomain.collectAsStateWithLifecycle()
    val journalTemplateId by viewModel.journalTemplateId.collectAsStateWithLifecycle()
    val learningTemplateId by viewModel.learningTemplateId.collectAsStateWithLifecycle()
    val researchTemplateId by viewModel.researchTemplateId.collectAsStateWithLifecycle()
    val meetingTemplateId by viewModel.meetingTemplateId.collectAsStateWithLifecycle()

    val apiToken by viewModel.apiToken.collectAsStateWithLifecycle()
    val notionDatabaseId by viewModel.notionDatabaseId.collectAsStateWithLifecycle()
    val titlePropertyName by viewModel.titlePropertyName.collectAsStateWithLifecycle()
    val notionTargetType by viewModel.notionTargetType.collectAsStateWithLifecycle()

    val suffixLabel = if (notionTargetType.lowercase() == "page") "Parent Page ID" else "Database ID"

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

            Spacer(modifier = Modifier.height(24.dp))

            // ── Section: Notion Integration ────────────────────────
            SectionHeader(title = "Notion Integration", icon = Icons.Outlined.CloudQueue)

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Notion Domain & Templates",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Workspace Domain
                    OutlinedTextField(
                        value = workspaceDomain,
                        onValueChange = { viewModel.updateWorkspaceDomain(it) },
                        label = { Text("Workspace Domain") },
                        placeholder = { Text("e.g. personal-manager") },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Journal Template ID
                    OutlinedTextField(
                        value = journalTemplateId,
                        onValueChange = { viewModel.updateJournalTemplateId(it) },
                        label = { Text("Journal Page/Template ID") },
                        placeholder = { Text("e.g. 1a2b3c...") },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Learning Template ID
                    OutlinedTextField(
                        value = learningTemplateId,
                        onValueChange = { viewModel.updateLearningTemplateId(it) },
                        label = { Text("Learning Page/Template ID") },
                        placeholder = { Text("e.g. 1a2b3c...") },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Research Template ID
                    OutlinedTextField(
                        value = researchTemplateId,
                        onValueChange = { viewModel.updateResearchTemplateId(it) },
                        label = { Text("Research Page/Template ID") },
                        placeholder = { Text("e.g. 1a2b3c...") },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Meeting Template ID
                    OutlinedTextField(
                        value = meetingTemplateId,
                        onValueChange = { viewModel.updateMeetingTemplateId(it) },
                        label = { Text("Meeting Page/Template ID") },
                        placeholder = { Text("e.g. 1a2b3c...") },
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Notion REST API Setup",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Notion Storage Target Type Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Save Target:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        
                        listOf("database", "page").forEach { typeOption ->
                            val isSelected = notionTargetType.lowercase() == typeOption
                            val selectedColor = AccentPrimary
                            FilterChip(
                                onClick = { viewModel.updateNotionTargetType(typeOption) },
                                selected = isSelected,
                                label = {
                                    Text(
                                        text = typeOption.replaceFirstChar { it.uppercase() },
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = selectedColor.copy(alpha = 0.2f),
                                    selectedLabelColor = selectedColor,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color.Transparent,
                                    selectedBorderColor = selectedColor.copy(alpha = 0.5f),
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    var showToken by remember { mutableStateOf(false) }

                    // Notion API Token
                    OutlinedTextField(
                        value = apiToken,
                        onValueChange = { viewModel.updateApiToken(it) },
                        label = { Text("Notion API Token") },
                        placeholder = { Text("secret_...") },
                        singleLine = true,
                        visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showToken = !showToken }) {
                                Icon(
                                    imageVector = if (showToken) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (showToken) "Hide token" else "Show token",
                                    tint = TextTertiary
                                )
                            }
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Notion Target ID (Database ID or Parent Page ID)
                    OutlinedTextField(
                        value = notionDatabaseId,
                        onValueChange = { viewModel.updateNotionDatabaseId(it) },
                        label = { Text(suffixLabel) },
                        placeholder = { Text(if (notionTargetType.lowercase() == "page") "e.g. 1a2b3c... (Parent Page ID)" else "e.g. 1a2b3c... (Database ID)") },
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

                    if (notionTargetType.lowercase() != "page") {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Title Property Name
                        OutlinedTextField(
                            value = titlePropertyName,
                            onValueChange = { viewModel.updateTitlePropertyName(it) },
                            label = { Text("Notion Title Property Name") },
                            placeholder = { Text("Default: Name") },
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

package com.productivity.app.ui.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.productivity.app.data.model.FocusTask
import com.productivity.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTaskBottomSheet(
    taskToEdit: FocusTask? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String?,
        targetMinutes: Int,
        workMinutes: Int,
        breakMinutes: Int,
        enableGradualScaling: Boolean,
        gradualIncrement: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }

    val initialHours = taskToEdit?.let { it.dailyTargetMinutes / 60 } ?: 1
    val initialMins = taskToEdit?.let { it.dailyTargetMinutes % 60 } ?: 0
    var targetHoursInput by remember { mutableStateOf(initialHours.toString()) }
    var targetMinutesInput by remember { mutableStateOf(initialMins.toString()) }

    var workMinutesInput by remember { mutableStateOf(taskToEdit?.workDurationMinutes?.toString() ?: "25") }
    var breakMinutesInput by remember { mutableStateOf(taskToEdit?.breakDurationMinutes?.toString() ?: "5") }

    var enableGradualScaling by remember { mutableStateOf(taskToEdit?.enableGradualScaling ?: false) }
    var gradualIncrementInput by remember { mutableStateOf(taskToEdit?.gradualMinutesIncrement?.toString() ?: "5") }

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
                text = if (taskToEdit == null) "New Focus Goal" else "Edit Focus Goal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

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

                // Daily Target
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

                // Presets
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

                // Gradual scaling
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

                // Increments
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

                // Action button
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

                            onSave(
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
                            text = if (taskToEdit == null) "Create Goal" else "Save Changes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

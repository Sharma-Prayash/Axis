package com.productivity.app.ui.tracker

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productivity.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTrackerScreen(
    viewModel: TrackerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("course") }
    var description by remember { mutableStateOf("") }
    val modules = remember { mutableStateListOf("") }

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is TrackerUiEvent.TrackerCreated -> onNavigateBack()
                is TrackerUiEvent.Error -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Tracker", fontWeight = FontWeight.SemiBold) },
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
                label = { Text("Tracker Title") },
                placeholder = { Text("e.g. Data Structures & Algorithms") },
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

            Spacer(modifier = Modifier.height(24.dp))

            // ── Type Selector ────────────────────────────────
            Text(
                text = "Type",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Course option
                TrackerTypeCard(
                    title = "Course",
                    description = "Track modules & lectures",
                    icon = Icons.Outlined.School,
                    color = InfoBlue,
                    isSelected = selectedType == "course",
                    onClick = { selectedType = "course" },
                    modifier = Modifier.weight(1f)
                )
                // Project option
                TrackerTypeCard(
                    title = "Project",
                    description = "Track milestones & tasks",
                    icon = Icons.Outlined.WorkOutline,
                    color = AccentTertiary,
                    isSelected = selectedType == "project",
                    onClick = { selectedType = "project" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Description ──────────────────────────────────
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                placeholder = { Text("What are you tracking?") },
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

            // ── Course Modules Input ─────────────────────────
            if (selectedType == "course") {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Modules List",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                modules.forEachIndexed { index, moduleTitle ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = moduleTitle,
                            onValueChange = { modules[index] = it },
                            placeholder = { Text("Module ${index + 1} Title") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = InfoBlue,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                focusedLabelColor = InfoBlue,
                                unfocusedLabelColor = TextTertiary,
                                cursorColor = InfoBlue,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { 
                                if (modules.size > 1) {
                                    modules.removeAt(index)
                                } else {
                                    modules[0] = ""
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete Module",
                                tint = AccentPrimary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { modules.add("") },
                    colors = ButtonDefaults.textButtonColors(contentColor = InfoBlue)
                ) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Module")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Create Button ────────────────────────────────
            Button(
                onClick = {
                    if (selectedType == "course") {
                        val nonEmptyModules = modules.filter { it.isNotBlank() }
                        viewModel.createTrackerWithModules(
                            title = title,
                            type = selectedType,
                            description = description,
                            modules = nonEmptyModules
                        )
                    } else {
                        viewModel.createTracker(
                            title = title,
                            type = selectedType,
                            description = description
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
                    Icon(
                        Icons.Outlined.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Tracker", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Type Selection Card ─────────────────────────────────────────

@Composable
private fun TrackerTypeCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.12f) else DarkSurface
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                // Using the built-in border with the selected color
            )
        } else null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) color else TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

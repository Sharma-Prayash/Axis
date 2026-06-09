package com.productivity.app.ui.checklist

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productivity.app.data.model.Checklist
import com.productivity.app.ui.theme.*
import kotlinx.coroutines.flow.first


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistListScreen(
    viewModel: ChecklistViewModel = hiltViewModel(),
    onNavigateToCreate: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {}
) {
    val activeChecklists by viewModel.activeChecklists.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Active Lists", "Templates")

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ChecklistUiEvent.TemplateDuplicated -> {
                    snackbarHostState.showSnackbar("Template instantiated ✓")
                    onNavigateToDetail(event.id)
                }
                is ChecklistUiEvent.Deleted -> {
                    snackbarHostState.showSnackbar("Checklist deleted")
                }
                is ChecklistUiEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = AccentPrimary,
                contentColor = DarkBackground,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Checklist")
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Title Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Checklists",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Tabs Selector
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = AccentPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = AccentPrimary
                    )
                },
                divider = {
                    HorizontalDivider(color = DarkSurfaceVariant)
                },
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = AccentPrimary,
                        unselectedContentColor = TextSecondary
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentPrimary)
                }
            } else {
                when (selectedTabIndex) {
                    0 -> ActiveListsTab(
                        checklists = activeChecklists,
                        viewModel = viewModel,
                        onNavigateToDetail = onNavigateToDetail
                    )
                    1 -> TemplatesTab(
                        templates = templates,
                        onDuplicate = { viewModel.duplicateTemplate(it) },
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveListsTab(
    checklists: List<Checklist>,
    viewModel: ChecklistViewModel,
    onNavigateToDetail: (Long) -> Unit
) {
    if (checklists.isEmpty()) {
        EmptyChecklistState(
            title = "No Active Lists",
            subtitle = "Tap + to create a new checklist or duplicate a template"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = checklists,
                key = { it.id }
            ) { checklist ->
                // Fetch stats from state
                val itemsFlow = remember(checklist.id) { viewModel.selectedChecklistItems }
                val itemsState = itemsFlow.collectAsStateWithLifecycle()
                
                // Track total items and completed items. Since selectedChecklistItems only loads for the selected one,
                // we should retrieve statistics from checklist table, or load them from database.
                // Wait! Since checklist doesn't store count statistics directly, let's query the database count dynamically.
                // A very clean approach is to let ChecklistCard execute a small LaunchedEffect to fetch counts.
                var stats by remember { mutableStateOf(Pair(0, 0)) } // Pair(checked, total)
                
                LaunchedEffect(checklist.id) {
                    viewModel.activeChecklists.value.find { it.id == checklist.id }?.let {
                        // Gather items count
                        viewModel.repository.getItemsForChecklist(checklist.id).collect { list ->
                            stats = Pair(list.count { it.isChecked }, list.size)
                        }
                    }
                }

                ChecklistCard(
                    checklist = checklist,
                    checkedCount = stats.first,
                    totalCount = stats.second,
                    onClick = { onNavigateToDetail(checklist.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun TemplatesTab(
    templates: List<Checklist>,
    onDuplicate: (Long) -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    if (templates.isEmpty()) {
        EmptyChecklistState(
            title = "No Templates",
            subtitle = "Create reusable checklists by setting 'Is Template' to true"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = templates,
                key = { it.id }
            ) { template ->
                TemplateCard(
                    template = template,
                    onClick = { onNavigateToDetail(template.id) },
                    onDuplicate = { onDuplicate(template.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ChecklistCard(
    checklist: Checklist,
    checkedCount: Int,
    totalCount: Int,
    onClick: () -> Unit
) {
    val typeColor = getChecklistTypeColor(checklist.type)
    val progress = if (totalCount > 0) checkedCount.toFloat() / totalCount else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getChecklistTypeIcon(checklist.type),
                    contentDescription = checklist.type,
                    tint = typeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = checklist.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$checkedCount of $totalCount items",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (progress == 1f) SuccessGreen else AccentPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (progress == 1f) SuccessGreen else AccentPrimary,
                    trackColor = DarkSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: Checklist,
    onClick: () -> Unit,
    onDuplicate: () -> Unit
) {
    val typeColor = getChecklistTypeColor(template.type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getChecklistTypeIcon(template.type),
                    contentDescription = template.type,
                    tint = typeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reusable ${template.type.replaceFirstChar { it.uppercase() }} Template",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Quick duplicate template button
            IconButton(
                onClick = onDuplicate,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AccentPrimary.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Duplicate Template",
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyChecklistState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(AccentPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Checklist,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = AccentPrimary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

// ── Helper Mappings ──────────────────────────────────────────────────

fun getChecklistTypeColor(type: String): Color = when (type.lowercase()) {
    "travel" -> SuccessGreen
    "shopping" -> AccentPrimary
    "event" -> MeetingColor
    else -> GeneralColor
}

fun getChecklistTypeIcon(type: String): ImageVector = when (type.lowercase()) {
    "travel" -> Icons.Outlined.Flight
    "shopping" -> Icons.Outlined.ShoppingCart
    "event" -> Icons.Outlined.CalendarToday
    else -> Icons.Outlined.ListAlt
}

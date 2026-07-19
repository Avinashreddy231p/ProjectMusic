package com.mardous.projectmusic.ui.screen.library.moods

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mardous.projectmusic.core.sort.MoodSortMode
import com.mardous.projectmusic.data.model.Mood
import com.mardous.projectmusic.ui.component.compose.expressive.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoodsScreen(
    viewModel: MoodsViewModel,
    onMoodClick: (Mood) -> Unit,
    onAssignSongs: () -> Unit,
    onBackClick: () -> Unit
) {
    val moodsMap by viewModel.moods.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedMoodIds by viewModel.selectedMoodIds.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val grouping by viewModel.grouping.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var showMetadataEditor by remember { mutableStateOf(false) }
    var editingMood by remember { mutableStateOf<Mood?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortGroupSheet by remember { mutableStateOf(false) }

    val isSelectionMode = selectedMoodIds.isNotEmpty()

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                SelectionTopAppBar(
                    selectedCount = selectedMoodIds.size,
                    onClearSelection = { viewModel.clearSelection() }
                )
            } else {
                ExpressiveSearchAppBar(
                    title = "Moods",
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.search(it) },
                    onVoiceSearchClick = { /* Handle voice search */ },
                    onBackClick = onBackClick,
                    isSearchActive = isSearchActive,
                    onSearchActiveChange = { isSearchActive = it }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Add Mood") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = { 
                    editingMood = null
                    showMetadataEditor = true 
                },
                expanded = !isSelectionMode,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                SelectionBottomBar(
                    onDelete = { viewModel.deleteSelectedMoods() },
                    onAssignSongs = onAssignSongs,
                    onMerge = { /* Implement merge if available */ }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (moodsMap.isEmpty() && searchQuery.isEmpty()) {
                EmptyMoodsState(onAddClick = { 
                    editingMood = null
                    showMetadataEditor = true 
                })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                ) {
                    item {
                        SortFilterHeader(
                            onFilterClick = { showFilterSheet = true },
                            onSortDropdownClick = { showSortGroupSheet = true }
                        )
                    }

                    moodsMap.forEach { (groupLabel, moodsInGroup) ->
                        stickyHeader {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            ) {
                                Text(
                                    text = groupLabel,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        item {
                            FlowRow(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                moodsInGroup.forEach { mood ->
                                    val isSelected = selectedMoodIds.contains(mood.id)
                                    var showCardMenu by remember { mutableStateOf(false) }

                                    Box(modifier = Modifier.fillMaxWidth(0.47f)) {
                                        ExpressiveLibraryCard(
                                            modifier = Modifier.animateItem(),
                                            content = LibraryCardContent.Mood(
                                                emoji = mood.emoji ?: "🎵",
                                                rank = mood.overallRank
                                            ),
                                            isSelected = isSelected,
                                            onClick = {
                                                if (isSelectionMode) {
                                                    viewModel.toggleSelection(mood.id)
                                                } else {
                                                    onMoodClick(mood)
                                                }
                                            },
                                            onLongClick = {
                                                viewModel.toggleSelection(mood.id)
                                            },
                                            onMenuClick = {
                                                showCardMenu = true
                                            }
                                        )

                                        DropdownMenu(
                                            expanded = showCardMenu,
                                            onDismissRequest = { showCardMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Edit") },
                                                onClick = {
                                                    showCardMenu = false
                                                    editingMood = mood
                                                    showMetadataEditor = true
                                                },
                                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Rename") },
                                                onClick = {
                                                    showCardMenu = false
                                                    editingMood = mood
                                                    showMetadataEditor = true
                                                },
                                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete") },
                                                onClick = {
                                                    showCardMenu = false
                                                    viewModel.deleteMood(mood.id)
                                                },
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                                colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error, leadingIconColor = MaterialTheme.colorScheme.error)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheets
    if (showMetadataEditor) {
        ExpressiveMetadataEditor(
            type = MetadataEditorType.Mood,
            initialName = editingMood?.name ?: "",
            initialEmoji = editingMood?.emoji ?: "🎵",
            initialDescription = editingMood?.description ?: "",
            onDismissRequest = { showMetadataEditor = false },
            onSave = { name, description, emoji, color, _, _ ->
                if (editingMood == null) {
                    viewModel.createMood(name, description, emoji, color)
                } else {
                    viewModel.updateMood(editingMood!!.copy(
                        name = name,
                        description = description,
                        emoji = emoji
                    ))
                }
                showMetadataEditor = false
            }
        )
    }

    if (showFilterSheet) {
        ExpressiveFilterSheet(
            state = filterState,
            onStateChange = { viewModel.updateFilter(it) },
            onReset = { viewModel.updateFilter(LibraryFilterState()) },
            onDismissRequest = { showFilterSheet = false }
        )
    }

    if (showSortGroupSheet) {
        SortGroupSheet(
            currentGrouping = grouping,
            onGroupingChange = { viewModel.updateGrouping(it) },
            onDismissRequest = { showSortGroupSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopAppBar(
    selectedCount: Int,
    onClearSelection: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "Clear Selection")
            }
        },
        actions = {
            IconButton(onClick = { /* Select All logic */ }) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select All")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun SelectionBottomBar(
    onDelete: () -> Unit,
    onAssignSongs: () -> Unit,
    onMerge: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Delete")
            }
            TextButton(onClick = onAssignSongs) {
                Icon(Icons.Default.LibraryMusic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Assign Songs")
            }
            TextButton(onClick = onMerge) {
                Icon(Icons.Default.Merge, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Merge")
            }
        }
    }
}

@Composable
private fun SortFilterHeader(
    onFilterClick: () -> Unit,
    onSortDropdownClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Mood Library",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Row {
            IconButton(onClick = onFilterClick) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
            }
            ExpressiveSplitButton(
                label = "Sort",
                icon = Icons.AutoMirrored.Filled.Sort,
                onActionClick = { /* Toggle Order */ },
                onDropdownClick = onSortDropdownClick
            )
        }
    }
}

@Composable
private fun EmptyMoodsState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Mood,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Your Mood Library is Empty",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Start by adding your first music mood.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAddClick,
            shape = MaterialTheme.shapes.extraLarge,
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Create First Mood")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortGroupSheet(
    currentGrouping: MoodGrouping,
    onGroupingChange: (MoodGrouping) -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Group By",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            MoodGrouping.entries.forEach { grouping ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGroupingChange(grouping) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = currentGrouping == grouping, onClick = { onGroupingChange(grouping) })
                    Spacer(Modifier.width(12.dp))
                    Text(grouping.name)
                }
            }
        }
    }
}

package com.mardous.projectmusic.ui.screen.library.instruments

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mardous.projectmusic.data.local.database.metadata.InstrumentFamily
import com.mardous.projectmusic.data.model.Instrument
import com.mardous.projectmusic.ui.component.compose.expressive.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InstrumentsScreen(
    viewModel: InstrumentsViewModel,
    onInstrumentClick: (Instrument) -> Unit,
    onAssignSongs: () -> Unit,
    onBackClick: () -> Unit
) {
    val instrumentsMap by viewModel.instruments.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedInstruments by viewModel.selectedInstruments.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val currentGrouping by viewModel.grouping.collectAsState()
    
    var isSearchActive by remember { mutableStateOf(false) }
    var isFilterSheetOpen by remember { mutableStateOf(false) }
    var isSortSheetOpen by remember { mutableStateOf(false) }
    var isEditorOpen by remember { mutableStateOf(false) }
    var editingInstrument by remember { mutableStateOf<Instrument?>(null) }

    val allInstruments = remember(instrumentsMap) { instrumentsMap.values.flatten() }

    Scaffold(
        topBar = {
            ExpressiveSearchAppBar(
                title = if (selectionMode) "${selectedInstruments.size} Selected" else "Instruments",
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.search(it) },
                onVoiceSearchClick = { /* Handle voice search */ },
                onBackClick = {
                    if (selectionMode) viewModel.toggleSelectionMode() else onBackClick()
                },
                isSearchActive = isSearchActive,
                onSearchActiveChange = { isSearchActive = it },
                actions = {
                    if (selectionMode) {
                        IconButton(onClick = { viewModel.selectAll(allInstruments.map { it.id }) }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                    } else {
                        IconButton(onClick = { isFilterSheetOpen = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!selectionMode) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingInstrument = null
                        isEditorOpen = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Instrument") },
                    expanded = true, // Can be linked to scroll state if needed
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectionMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                BottomAppBar(
                    actions = {
                        IconButton(onClick = { /* Share selected */ }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = onAssignSongs) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = "Assign Songs")
                        }
                        IconButton(onClick = { /* Export selected */ }) {
                            Icon(Icons.Default.IosShare, contentDescription = "Export")
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { 
                                selectedInstruments.forEach { viewModel.deleteInstrument(it) }
                                viewModel.toggleSelectionMode()
                            },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (allInstruments.isEmpty()) {
                EmptyInstrumentsState(
                    onAddClick = {
                        editingInstrument = null
                        isEditorOpen = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            ExpressiveSplitButton(
                                label = currentGrouping.name,
                                icon = Icons.AutoMirrored.Filled.Sort,
                                onActionClick = { isSortSheetOpen = true },
                                onDropdownClick = { isSortSheetOpen = true }
                            )
                        }
                    }

                    instrumentsMap.forEach { (groupName, groupInstruments) ->
                        if (groupInstruments.isNotEmpty()) {
                            stickyHeader {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = groupName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            items(groupInstruments, key = { it.id }) { instrument ->
                                var showMenu by remember { mutableStateOf(false) }

                                Box {
                                    ExpressiveLibraryCard(
                                        content = LibraryCardContent.Instrument(
                                            icon = instrument.family.toIcon(),
                                            name = instrument.name,
                                            family = instrument.family.name,
                                            listeningTime = "${instrument.songCount} songs",
                                            playCount = instrument.playCount.toInt()
                                        ),
                                        isSelected = selectedInstruments.contains(instrument.id),
                                        onLongClick = {
                                            if (!selectionMode) viewModel.toggleSelectionMode()
                                            viewModel.toggleInstrumentSelection(instrument.id)
                                        },
                                        onMenuClick = { showMenu = true },
                                        onClick = {
                                            if (selectionMode) {
                                                viewModel.toggleInstrumentSelection(instrument.id)
                                            } else {
                                                onInstrumentClick(instrument)
                                            }
                                        }
                                    )

                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit") },
                                            onClick = {
                                                showMenu = false
                                                editingInstrument = instrument
                                                isEditorOpen = true
                                            },
                                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Rename") },
                                            onClick = {
                                                showMenu = false
                                                // Simplified rename logic, can use editor
                                                editingInstrument = instrument
                                                isEditorOpen = true
                                            },
                                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) }
                                        )
                                        Divider()
                                        DropdownMenuItem(
                                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                showMenu = false
                                                viewModel.deleteInstrument(instrument.id)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
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

    // Bottom Sheets
    if (isEditorOpen) {
        ExpressiveMetadataEditor(
            type = MetadataEditorType.Instrument,
            onDismissRequest = { isEditorOpen = false },
            onSave = { name, description, _, color, _, family ->
                if (editingInstrument == null) {
                    viewModel.createInstrument(name, description, family!!, color)
                } else {
                    viewModel.updateInstrument(editingInstrument!!.copy(
                        name = name,
                        description = description,
                        family = family!!,
                        color = color
                    ))
                }
                isEditorOpen = false
            },
            existingNames = allInstruments.map { it.name },
            initialName = editingInstrument?.name ?: "",
            initialDescription = editingInstrument?.description ?: "",
            initialColor = editingInstrument?.color,
            initialFamily = editingInstrument?.family ?: InstrumentFamily.Strings
        )
    }

    if (isFilterSheetOpen) {
        ExpressiveFilterSheet(
            state = filterState,
            onStateChange = { viewModel.updateFilterState(it) },
            onReset = { viewModel.updateFilterState(LibraryFilterState()) },
            onDismissRequest = { isFilterSheetOpen = false }
        )
    }

    if (isSortSheetOpen) {
        SortGroupSheet(
            currentGrouping = currentGrouping,
            onGroupingChange = { viewModel.updateGrouping(it) },
            onDismissRequest = { isSortSheetOpen = false }
        )
    }
}

@Composable
fun EmptyInstrumentsState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Instruments Found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start by adding your favorite instruments to organize your music library better.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAddClick,
            shape = MaterialTheme.shapes.large,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Your First Instrument")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortGroupSheet(
    currentGrouping: InstrumentGrouping,
    onGroupingChange: (InstrumentGrouping) -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp, top = 8.dp)
        ) {
            Text(
                text = "Group & Sort",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "Group By",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            InstrumentGrouping.entries.forEach { grouping ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentGrouping == grouping,
                        onClick = { onGroupingChange(grouping) }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = grouping.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun InstrumentFamily.toIcon(): ImageVector = when (this) {
    InstrumentFamily.Strings -> Icons.Default.MusicNote
    InstrumentFamily.Percussion -> Icons.Default.MusicNote
    InstrumentFamily.Brass -> Icons.Default.MusicNote
    InstrumentFamily.Woodwind -> Icons.Default.MusicNote
    InstrumentFamily.Keyboard -> Icons.Default.Piano
    InstrumentFamily.Electronic -> Icons.Default.Memory
}

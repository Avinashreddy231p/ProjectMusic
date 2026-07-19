package com.mardous.projectmusic.ui.screen.library.tags

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
import com.mardous.projectmusic.data.local.database.metadata.TagCategory
import com.mardous.projectmusic.data.model.Tag
import com.mardous.projectmusic.ui.component.compose.expressive.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TagsScreen(
    viewModel: TagsViewModel,
    onTagClick: (Tag) -> Unit,
    onAssignSongs: () -> Unit,
    onBackClick: () -> Unit
) {
    val tagsMap by viewModel.tags.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTagIds by viewModel.selectedTagIds.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val grouping by viewModel.grouping.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var showMetadataEditor by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<Tag?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortGroupSheet by remember { mutableStateOf(false) }

    val isSelectionMode = selectedTagIds.isNotEmpty()

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                SelectionTopAppBar(
                    selectedCount = selectedTagIds.size,
                    onClearSelection = { viewModel.clearSelection() }
                )
            } else {
                ExpressiveSearchAppBar(
                    title = "Tags",
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
                text = { Text("Add Tag") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {
                    editingTag = null
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
                    onDelete = { viewModel.deleteSelectedTags() },
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
            if (tagsMap.isEmpty() && searchQuery.isEmpty()) {
                EmptyTagsState(onAddClick = {
                    editingTag = null
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

                    if (grouping == TagGrouping.Category) {
                        item {
                            val selectedCategory = filterState.selectedTagCategories.firstOrNull() ?: TagCategory.Activity
                            ExpressiveSegmentedButton(
                                options = viewModel.categories,
                                selectedOption = selectedCategory,
                                onOptionSelect = { category: TagCategory ->
                                    viewModel.updateFilter(filterState.copy(selectedTagCategories = setOf(category)))
                                },
                                labelProvider = { it.name },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    tagsMap.forEach { (groupLabel, tagsInGroup) ->
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
                                tagsInGroup.forEach { tag ->
                                    val isSelected = selectedTagIds.contains(tag.id)
                                    var showCardMenu by remember { mutableStateOf(false) }

                                    Box(modifier = Modifier.fillMaxWidth(0.47f)) {
                                        ExpressiveLibraryCard(
                                            modifier = Modifier.animateItem(),
                                            content = LibraryCardContent.Tag(
                                                emoji = tag.icon ?: "🏷️",
                                                name = tag.name,
                                                category = tag.category.name,
                                                listeningTime = formatDuration(tag.listenedDurationMs),
                                                playCount = tag.playCount.toInt()
                                            ),
                                            isSelected = isSelected,
                                            onClick = {
                                                if (isSelectionMode) {
                                                    viewModel.toggleSelection(tag.id)
                                                } else {
                                                    onTagClick(tag)
                                                }
                                            },
                                            onLongClick = {
                                                viewModel.toggleSelection(tag.id)
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
                                                    editingTag = tag
                                                    showMetadataEditor = true
                                                },
                                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Rename") },
                                                onClick = {
                                                    showCardMenu = false
                                                    editingTag = tag
                                                    showMetadataEditor = true
                                                },
                                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete") },
                                                onClick = {
                                                    showCardMenu = false
                                                    viewModel.deleteTag(tag.id)
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

    if (showMetadataEditor) {
        ExpressiveMetadataEditor(
            type = MetadataEditorType.Tag,
            initialName = editingTag?.name ?: "",
            initialEmoji = editingTag?.icon ?: "🏷️",
            initialDescription = editingTag?.description ?: "",
            initialCategory = editingTag?.category ?: TagCategory.Activity,
            onDismissRequest = { showMetadataEditor = false },
            onSave = { name, description, emoji, color, category, _ ->
                if (editingTag == null) {
                    viewModel.createTag(name, description, category ?: TagCategory.Activity, color)
                } else {
                    viewModel.updateTag(editingTag!!.copy(
                        name = name,
                        description = description,
                        icon = emoji,
                        category = category ?: editingTag!!.category,
                        color = color
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

@Composable
private fun formatDuration(durationMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
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
            text = "Tag Library",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Row {
            IconButton(onClick = onFilterClick) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
            }
            ExpressiveSplitButton(
                label = "Group",
                icon = Icons.AutoMirrored.Filled.Sort,
                onActionClick = onSortDropdownClick,
                onDropdownClick = onSortDropdownClick
            )
        }
    }
}

@Composable
private fun EmptyTagsState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Tag,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Your Tag Library is Empty",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Start by adding your first music tag.",
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
            Text("Create First Tag")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortGroupSheet(
    currentGrouping: TagGrouping,
    onGroupingChange: (TagGrouping) -> Unit,
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

            TagGrouping.entries.forEach { grouping ->
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
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

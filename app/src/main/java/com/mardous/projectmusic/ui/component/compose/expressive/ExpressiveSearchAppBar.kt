package com.mardous.projectmusic.ui.component.compose.expressive

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveSearchAppBar(
    title: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onVoiceSearchClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {},
    placeholder: String = "Search...",
    voiceSearchPlaceholder: String = "Speak to search...",
    recentSearches: List<String> = emptyList(),
    liveSuggestions: List<String> = emptyList(),
    onSuggestionClick: (String) -> Unit = {},
    onClearRecentSearch: (String) -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    AnimatedContent(
        targetState = isSearchActive,
        transitionSpec = {
            fadeIn(animationSpec = tween(400)) togetherWith
                    fadeOut(animationSpec = tween(400))
        },
        label = "AppBarSearchTransition",
        modifier = modifier.fillMaxWidth()
    ) { active ->
        if (active) {
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearch = { /* Handle search committed */ },
                active = true,
                onActiveChange = onSearchActiveChange,
                placeholder = {
                    Text(
                        text = if (searchQuery.isEmpty()) voiceSearchPlaceholder else placeholder,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal
                    )
                },
                leadingIcon = {
                    IconButton(onClick = { onSearchActiveChange(false) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                        IconButton(onClick = onVoiceSearchClick) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Search")
                        }
                    }
                },
                shape = CircleShape,
                colors = SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp)
            ) {
                SuggestionsList(
                    searchQuery = searchQuery,
                    recentSearches = recentSearches,
                    liveSuggestions = liveSuggestions,
                    onSuggestionClick = onSuggestionClick,
                    onClearRecentSearch = onClearRecentSearch
                )
            }
        } else {
            LargeTopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onSearchActiveChange(true) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    actions()
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@Composable
private fun SuggestionsList(
    searchQuery: String,
    recentSearches: List<String>,
    liveSuggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    onClearRecentSearch: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (searchQuery.isEmpty() && recentSearches.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Searches",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(recentSearches) { search ->
                ListItem(
                    leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                    trailingContent = {
                        IconButton(onClick = { onClearRecentSearch(search) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    },
                    modifier = Modifier.clickable { onSuggestionClick(search) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) {
                    Text(
                        text = search,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        } else if (searchQuery.isNotEmpty()) {
            items(liveSuggestions) { suggestion ->
                ListItem(
                    leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.clickable { onSuggestionClick(suggestion) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

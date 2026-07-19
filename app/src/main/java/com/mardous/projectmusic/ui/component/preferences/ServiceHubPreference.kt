package com.mardous.projectmusic.ui.component.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.mardous.projectmusic.ui.theme.ProjectMusicTheme

class ServiceHubPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    init {
        layoutResource = com.mardous.projectmusic.R.layout.preference_compose_host
    }

    // Callbacks
    var onPrimaryClick: () -> Unit = {}
    var onScrobblingEnabledChange: (Boolean) -> Unit = {}
    var onNowPlayingEnabledChange: (Boolean) -> Unit = {}
    var onOfflineScrobblingChange: (Boolean) -> Unit = {}
    var onScrobblePercentageChange: (Int) -> Unit = {}
    var onSyncFavoritesChange: (Boolean) -> Unit = {}
    var onInfoEnabledChange: (Boolean) -> Unit = {}
    var onPendingScrobblesClick: () -> Unit = {}

    // State
    var serviceIconRes by mutableStateOf<Int?>(null)
    var serviceName by mutableStateOf("")
    var userName by mutableStateOf<String?>(null)
    var isExpanded by mutableStateOf(false)

    // Sub-settings state
    var scrobblingEnabled by mutableStateOf(false)
    var nowPlayingEnabled by mutableStateOf(false)
    var offlineScrobbling by mutableStateOf(false)
    var scrobblePercentage by mutableStateOf(50)
    var syncFavorites by mutableStateOf(false)
    var infoEnabled by mutableStateOf(true)
    var pendingScrobblesCount by mutableStateOf(0)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val composeView = holder.findViewById(com.mardous.projectmusic.R.id.compose_view) as? ComposeView ?: return
        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ProjectMusicTheme {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        HubContent()
                    }
                }
            }
        }
    }

    @Composable
    private fun HubContent() {
        val shape = MaterialTheme.shapes.large
        val chevronRotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "chevronRotation")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary Section (Profile/Connect)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shape.copy(topEnd = CornerSize(4.dp), bottomEnd = CornerSize(4.dp)))
                        .clickable { onPrimaryClick() },
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        serviceIconRes?.let {
                            Icon(painter = painterResource(it), contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                serviceName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            userName?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            } ?: Text(
                                "Connect account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Dropdown Toggle Section
                Surface(
                    modifier = Modifier
                        .width(64.dp)
                        .fillMaxHeight()
                        .clip(shape.copy(topStart = CornerSize(4.dp), bottomStart = CornerSize(4.dp)))
                        .clickable { isExpanded = !isExpanded },
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Expand settings",
                            modifier = Modifier.rotate(chevronRotation)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    HubSwitch(
                        title = "Enable Scrobbling",
                        checked = scrobblingEnabled,
                        onCheckedChange = onScrobblingEnabledChange
                    )

                    HubSwitch(
                        title = "Update Now Playing",
                        summary = "Show currently playing track on profile",
                        checked = nowPlayingEnabled,
                        onCheckedChange = onNowPlayingEnabledChange
                    )

                    if (serviceName == "Last.fm") {
                        HubSwitch(
                            title = "Offline Scrobbling",
                            summary = "Cache scrobbles when offline",
                            checked = offlineScrobbling,
                            onCheckedChange = onOfflineScrobblingChange
                        )

                        HubSwitch(
                            title = "Sync Favorites",
                            summary = "Sync loved tracks with Last.fm",
                            checked = syncFavorites,
                            onCheckedChange = onSyncFavoritesChange
                        )

                        HubSwitch(
                            title = "Download Biographies",
                            summary = "Fetch artist and album info",
                            checked = infoEnabled,
                            onCheckedChange = onInfoEnabledChange
                        )

                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "Scrobble completion: $scrobblePercentage%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Slider(
                                value = scrobblePercentage.toFloat(),
                                onValueChange = { onScrobblePercentageChange(it.toInt()) },
                                valueRange = 10f..100f,
                                steps = 17
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onPendingScrobblesClick() },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Pending Scrobbles", style = MaterialTheme.typography.bodyLarge)
                                    if (pendingScrobblesCount > 0) {
                                        Text("$pendingScrobblesCount items waiting to sync", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun HubSwitch(
        title: String,
        summary: String? = null,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                summary?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

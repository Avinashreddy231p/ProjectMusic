package com.mardous.projectmusic.ui.component.compose.preferences

import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mardous.projectmusic.R
import com.mardous.projectmusic.data.model.network.LoginState
import com.mardous.projectmusic.data.model.network.ScrobblingService
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.Icons

@Composable
fun PreferenceCategoryHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun DashboardCategoryHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScrobblingServiceHub(
    service: ScrobblingService,
    loginState: LoginState,
    enabled: Boolean,
    nowPlaying: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onNowPlayingChange: (Boolean) -> Unit,
    onPrimaryClick: () -> Unit,
    onPendingScrobblesClick: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val serviceName = if (service == ScrobblingService.Lastfm) "Last.fm" else "ListenBrainz"
    val iconRes = R.drawable.ic_person_24dp

    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Surface(
            shape = if (expanded) RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp) 
                    else MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = serviceName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    val userText = when (loginState) {
                        is LoginState.LoggedIn -> loginState.username
                        is LoginState.LoggingIn -> "Logging in..."
                        else -> "Not connected"
                    }
                    Text(text = userText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrimaryClick) {
                        Icon(painter = painterResource(if (loginState is LoginState.LoggedIn) R.drawable.ic_person_24dp else R.drawable.ic_cloud_24dp), contentDescription = null)
                    }
                    
                    Box(modifier = Modifier.size(1.dp, 32.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    
                    IconButton(onClick = { expanded = !expanded }) {
                        val rotation by animateFloatAsState(if (expanded) 180f else 0f)
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_drop_down_24dp),
                            contentDescription = "Expand",
                            modifier = Modifier.rotate(rotation),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Surface(
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HubSwitch(title = "Enable Scrobbling", checked = enabled, onCheckedChange = onEnabledChange)
                    HubSwitch(title = "Update Now Playing", summary = "Display active track on profile", checked = nowPlaying, onCheckedChange = onNowPlayingChange)
                    
                    if (service == ScrobblingService.Lastfm && onPendingScrobblesClick != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        TextButton(
                            onClick = onPendingScrobblesClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.History, null)
                            Spacer(Modifier.width(8.dp))
                            Text("View Pending Scrobbles")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HubSwitch(title: String, summary: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressivePreferenceItem(
    title: String,
    summary: String? = null,
    icon: Any? = null,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (enabled) 1f else 0.38f)),
                    contentAlignment = Alignment.Center
                ) {
                    when (icon) {
                        is Int -> Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = if (enabled) 1f else 0.38f),
                            modifier = Modifier.size(24.dp)
                        )
                        is ImageVector -> Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = if (enabled) 1f else 0.38f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = if (enabled) 1f else 0.38f)
                )
                if (!summary.isNullOrEmpty()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.38f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(16.dp))
                CompositionLocalProvider(
                    LocalContentColor provides contentColor.copy(alpha = if (enabled) 1f else 0.38f)
                ) {
                    trailingContent()
                }
            }
        }
    }
}

@Composable
fun ExpressiveSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
    icon: Any? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    ExpressivePreferenceItem(
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange, // Trust event bubbling to avoid double triggers
                enabled = enabled
            )
        },
        modifier = modifier
    )
}

@Composable
fun ExpressiveSliderItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    icon: Any? = null,
    valueDisplay: (Float) -> String = { it.toInt().toString() },
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (enabled) 1f else 0.38f)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (icon) {
                            is Int -> Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
                            is ImageVector -> Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = valueDisplay(value),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                enabled = enabled,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SplitButtonPreference(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Any? = null,
    enabled: Boolean = true,
    expanded: Boolean = false,
    expandableContent: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Surface(
            shape = if (expanded) RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp) 
                    else MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        when (icon) {
                            is Int -> Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
                            is ImageVector -> Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (!summary.isNullOrEmpty()) {
                        Text(text = summary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        enabled = enabled
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Box(modifier = Modifier.size(1.dp, 32.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    
                    IconButton(onClick = onExpandClick) {
                        val rotation by animateFloatAsState(if (expanded) 180f else 0f)
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_drop_down_24dp),
                            contentDescription = "Expand",
                            modifier = Modifier.rotate(rotation),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    expandableContent()
                }
            }
        }
    }
}

@Composable
fun SegmentedPreferenceItem(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icon: Any? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                when (icon) {
                    is Int -> Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
                    is ImageVector -> Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedIndex == index,
                    onClick = { onOptionSelected(index) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                ) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
fun SegmentedPreferenceGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun SegmentedPreferenceItem(
    title: String,
    summary: String? = null,
    icon: Any? = null,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (icon) {
                        is Int -> Icon(painter = painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        is ImageVector -> Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (!summary.isNullOrEmpty()) {
                    Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailingContent()
            }
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun SliderPreferenceItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    icon: Any? = null,
    valueDisplay: (Float) -> String = { it.toInt().toString() }
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            when (icon) {
                                is Int -> Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
                                is ImageVector -> Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = valueDisplay(value),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    steps = steps,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// Extension to help with background color in Row
@Composable
private fun Modifier.background(color: androidx.compose.ui.graphics.Color) = this.then(
    Modifier.drawBehind {
        drawRect(color)
    }
)

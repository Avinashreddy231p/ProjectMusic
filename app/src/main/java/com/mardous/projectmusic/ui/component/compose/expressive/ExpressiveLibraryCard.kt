package com.mardous.projectmusic.ui.component.compose.expressive

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource

enum class LibraryCardVariant {
    Tonal, Elevated
}

sealed interface LibraryCardContent {
    data class Mood(val emoji: String, val rank: Int) : LibraryCardContent
    data class Tag(
        val emoji: String,
        val name: String,
        val category: String,
        val listeningTime: String,
        val playCount: Int
    ) : LibraryCardContent
    data class Instrument(
        val icon: ImageVector,
        val name: String,
        val family: String,
        val listeningTime: String,
        val playCount: Int
    ) : LibraryCardContent
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ExpressiveLibraryCard(
    content: LibraryCardContent,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    variant: LibraryCardVariant = LibraryCardVariant.Tonal,
    isSelected: Boolean = false,
    enabled: Boolean = true
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "selection_scale"
    )

    val cardColors = when (variant) {
        LibraryCardVariant.Tonal -> CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
        LibraryCardVariant.Elevated -> CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                             else MaterialTheme.colorScheme.surface
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ),
        colors = cardColors,
        shape = MaterialTheme.shapes.extraLarge, // Expressive irregular corners
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (variant == LibraryCardVariant.Elevated) (if (isSelected) 8.dp else 2.dp) else 0.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background effect for Mood (Glassmorphism)
            if (content is LibraryCardContent.Mood) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(16.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                )
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                when (content) {
                    is LibraryCardContent.Mood -> MoodLayout(content)
                    is LibraryCardContent.Tag -> TagLayout(content)
                    is LibraryCardContent.Instrument -> InstrumentLayout(content)
                }
            }

            // Quick Menu
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Quick Menu",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Selection Checkmark
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(initialScale = 0.5f) + fadeIn(),
                exit = scaleOut(targetScale = 0.5f) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun MoodLayout(mood: LibraryCardContent.Mood) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = mood.emoji,
            fontSize = 72.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Rank #${mood.rank}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun TagLayout(tag: LibraryCardContent.Tag) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = tag.emoji, fontSize = 32.sp)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(tag.category, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            StatsSection(tag.listeningTime, tag.playCount)
        }
    }
}

@Composable
private fun InstrumentLayout(instrument: LibraryCardContent.Instrument) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = instrument.icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = instrument.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(instrument.family, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            StatsSection(instrument.listeningTime, instrument.playCount)
        }
    }
}

@Composable
private fun StatsSection(time: String, count: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$count plays",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { 0.7f }, // Placeholder for actual progress logic
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    }
}

package com.mardous.booming.ui.screen.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.data.local.room.ListeningHistoryDao
import com.mardous.booming.ui.component.compose.CollapsibleAppBarScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    CollapsibleAppBarScaffold(
        title = "Listening Stats",
        onBackClick = onBackClick,
        actions = {}
    ) { padding ->
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .wrapContentSize(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { SummaryCards(summary = uiState.summary) }

                if (uiState.topSongs.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Top Songs")
                    }
                    itemsIndexed(uiState.topSongs) { index, stat ->
                        RankedRow(rank = index + 1, name = "${stat.songTitle} - ${stat.artistName}", count = stat.playCount.toLong())
                    }
                }

                if (uiState.topArtists.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Top Artists")
                    }
                    itemsIndexed(uiState.topArtists) { index, stat ->
                        RankedRow(rank = index + 1, name = stat.artistName, count = stat.playCount.toLong())
                    }
                }

                if (uiState.topAlbums.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Top Albums")
                    }
                    itemsIndexed(uiState.topAlbums) { index, stat ->
                        RankedRow(rank = index + 1, name = "${stat.albumName} - ${stat.artistName}", count = stat.playCount.toLong())
                    }
                }

                if (uiState.topGenres.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Top Genres")
                    }
                    itemsIndexed(uiState.topGenres) { index, stat ->
                        RankedRow(rank = index + 1, name = stat.genre, count = stat.playCount.toLong())
                    }
                }

                if (uiState.monthlyStats.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Monthly Activity")
                    }
                    items(uiState.monthlyStats) { stat ->
                        MonthlyRow(month = stat.period, plays = stat.sessionCount.toLong(), timeMs = stat.totalPlayed)
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.exportCsv() },
                            enabled = !uiState.isExporting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (uiState.isExporting) "Exporting..." else "Export as CSV")
                        }
                        OutlinedButton(
                            onClick = { viewModel.exportJson() },
                            enabled = !uiState.isExporting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (uiState.isExporting) "Exporting..." else "Export as JSON")
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SummaryCards(summary: StatsSummaryUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(
            title = "Total Plays",
            value = summary.totalSessions.toString(),
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "Total Time",
            value = formatDuration(summary.totalTimeMs),
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(
            title = "Unique Songs",
            value = summary.uniqueSongs.toString(),
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "Completion Rate",
            value = "${(summary.completionRate * 100).toInt()}%",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun RankedRow(
    rank: Int,
    name: String,
    count: Long
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count plays",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonthlyRow(
    month: String,
    plays: Long,
    timeMs: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = month,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$plays plays",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = formatDuration(timeMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (plays > 0) 1f else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours}h ${minutes}min"
    } else {
        "${minutes}min"
    }
}

/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.projectmusic.ui.screen.about

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.mardous.projectmusic.App
import com.mardous.projectmusic.BuildConfig
import com.mardous.projectmusic.R
import com.mardous.projectmusic.core.model.about.AboutItemData
import com.mardous.projectmusic.core.model.about.Contribution
import com.mardous.projectmusic.extensions.MIME_TYPE_PLAIN_TEXT
import com.mardous.projectmusic.extensions.openUrl
import com.mardous.projectmusic.extensions.toChooser
import com.mardous.projectmusic.extensions.tryStartActivity
import com.mardous.projectmusic.util.Constants.APP_GITHUB_URL
import com.mardous.projectmusic.util.Constants.AUTHOR_GITHUB_URL
import com.mardous.projectmusic.util.Constants.COMMUNITY_LINK
import com.mardous.projectmusic.util.Constants.DONATION_LINK
import com.mardous.projectmusic.util.Constants.DOWNLOAD_URL
import com.mardous.projectmusic.util.Constants.FAQ_LINK
import com.mardous.projectmusic.util.Constants.ISSUE_TRACKER_LINK
import com.mardous.projectmusic.util.Constants.RELEASES_LINK
import com.mardous.projectmusic.util.Constants.SUPPORT_EMAIL
import com.mardous.projectmusic.util.Constants.TELEGRAM_LINK
import com.mardous.projectmusic.util.Constants.TRANSLATIONS_LINK
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val appVersion = try {
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "Unknown"
    } catch (_: PackageManager.NameNotFoundException) {
        "Unknown"
    }

    var showTranslatorsDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    val translators by produceState(emptyList<AboutItemData>()) {
        value = Contribution.loadContributions(context, "translators.json").map {
            AboutItemData(
                icon = { Icon(Icons.Default.Translate, contentDescription = null) },
                title = it.name,
                markdown = it.description,
                onClick = {}
            )
        }
    }

    if (showTranslatorsDialog) {
        ModalBottomSheet(onDismissRequest = { showTranslatorsDialog = false }) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.translators_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                itemsIndexed(translators) { index, item ->
                    AboutListItem(
                        index = index,
                        itemCount = translators.size,
                        data = item,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .8f)
                    )
                }
            }
        }
    }

    val libraries by produceLibraries(R.raw.aboutlibraries)
    if (showLicensesDialog) {
        ModalBottomSheet(onDismissRequest = { showLicensesDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                LibrariesContainer(
                    libraries = libraries,
                    licenseDialogConfirmText = stringResource(R.string.close_action),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                )
            }
        }
    }

    if (showDiagnosticsDialog) {
        ModalBottomSheet(onDismissRequest = { showDiagnosticsDialog = false }) {
            AboutDiagnosticsSheet()
        }
    }

    // Prepare lists outside of LazyColumn (Scaffold scope is composable)
    val dawidName = stringResource(R.string.contributor_dawid)
    val dawidSummary = stringResource(R.string.contributor_dawid_description)
    val lenardName = stringResource(R.string.contributor_lenard)
    val lenardSummary = stringResource(R.string.contributor_lenard_description)
    val translatorsTitle = stringResource(R.string.translators_title)
    val translatorsSummary = stringResource(R.string.translators_summary)
    val moreContributorsTitle = stringResource(R.string.more_contributors_title)
    val moreContributorsSummary = stringResource(R.string.more_contributors_summary)

    val contributors = remember(dawidName, dawidSummary, lenardName, lenardSummary, translatorsTitle, translatorsSummary, moreContributorsTitle, moreContributorsSummary) {
        listOf(
            AboutItemData(
                icon = { AboutContributorImage(username = "dawid") },
                title = dawidName,
                summary = dawidSummary,
                onClick = { context.openUrl("https://github.com/hackzy01") }
            ),
            AboutItemData(
                icon = { AboutContributorImage(username = "lenard") },
                title = lenardName,
                summary = lenardSummary,
                onClick = { context.openUrl("https://github.com/lenardflx") }
            ),
            AboutItemData(
                icon = { Icon(Icons.Default.Translate, null) },
                title = translatorsTitle,
                summary = translatorsSummary,
                badge = "Help us",
                onClick = { showTranslatorsDialog = true }
            ),
            AboutItemData(
                icon = { Icon(Icons.Default.Groups, null) },
                title = moreContributorsTitle,
                summary = moreContributorsSummary,
                onClick = { context.openUrl(COMMUNITY_LINK) }
            )
        )
    }

    val reportBugsTitle = stringResource(R.string.report_bugs)
    val reportBugsSummary = stringResource(R.string.report_bugs_summary)
    val helpTranslationsTitle = stringResource(R.string.help_with_translations)
    val helpTranslationsSummary = stringResource(R.string.help_with_translations_summary)

    val supportItems = remember(reportBugsTitle, reportBugsSummary, helpTranslationsTitle, helpTranslationsSummary) {
        listOf(
            AboutItemData(
                icon = { Icon(Icons.Default.BugReport, null) },
                title = reportBugsTitle,
                summary = reportBugsSummary,
                onClick = { context.openUrl(ISSUE_TRACKER_LINK) }
            ),
            AboutItemData(
                icon = { Icon(Icons.Default.Language, null) },
                title = helpTranslationsTitle,
                summary = helpTranslationsSummary,
                onClick = { context.openUrl(TRANSLATIONS_LINK) }
            ),
            AboutItemData(
                icon = { Icon(Icons.Default.Translate, null) },
                title = "Telegram Community",
                summary = "Join our community group",
                onClick = { context.openUrl(TELEGRAM_LINK) }
            )
        )
    }

    val licensesTitleText = stringResource(R.string.licenses)
    val legalItems = remember(licensesTitleText) {
        listOf(
            AboutItemData(
                icon = { Icon(Icons.Default.Description, null) },
                title = licensesTitleText,
                summary = "Open source libraries and licenses",
                onClick = { showLicensesDialog = true }
            ),
            AboutItemData(
                icon = { Icon(Icons.Default.PrivacyTip, null) },
                title = "Privacy Policy",
                summary = "How we handle your data",
                onClick = { context.openUrl("$APP_GITHUB_URL/blob/master/PRIVACY_POLICY.md") }
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDiagnosticsDialog = true }) {
                        Icon(Icons.Default.SettingsSuggest, contentDescription = "Diagnostics")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { contentPadding ->
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(500)
            )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                item {
                    AboutHeroHeader(
                        version = appVersion,
                        onChangelogClick = { context.openUrl(RELEASES_LINK) }
                    )
                }

                item { AboutSectionTitle(stringResource(R.string.author)) }
                item {
                    AuthorSectionExpressive(
                        onGitHubClick = { context.openUrl(AUTHOR_GITHUB_URL) },
                        onEmailClick = {
                            context.tryStartActivity(
                                Intent(Intent.ACTION_SENDTO)
                                    .setData("mailto:".toUri())
                                    .putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                                    .putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        "Project Music - Support & questions"
                                    )
                            )
                        },
                        onDonateClick = { context.openUrl(DONATION_LINK) }
                    )
                }

                item { AboutSectionTitle(stringResource(R.string.contributors)) }
                itemsIndexed(contributors) { index, item ->
                    AboutListItem(
                        index = index,
                        itemCount = contributors.size,
                        data = item,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item { AboutSectionTitle(stringResource(R.string.support_development)) }
                itemsIndexed(supportItems) { index, item ->
                    AboutListItem(
                        index = index,
                        itemCount = supportItems.size,
                        data = item,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item { AboutSectionTitle(licensesTitleText) }
                itemsIndexed(legalItems) { index, item ->
                    AboutListItem(
                        index = index,
                        itemCount = legalItems.size,
                        data = item,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Made with ❤️ for music lovers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Project Music © 2026",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AboutHeroHeader(
    version: String,
    onChangelogClick: () -> Unit
) {
    val context = LocalContext.current
    val invitationMessage = stringResource(R.string.invitation_message_content, DOWNLOAD_URL)
    val chooserTitle = stringResource(R.string.send_invitation_message)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 8.dp
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.app_description),
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Version",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = version,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Stable",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simulated Split Button look
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Button(
                    onClick = {
                        context.tryStartActivity(
                            Intent(Intent.ACTION_SEND)
                                .putExtra(Intent.EXTRA_TEXT, invitationMessage)
                                .setType(MIME_TYPE_PLAIN_TEXT)
                                .toChooser(chooserTitle)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share App")
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                        .align(Alignment.CenterVertically)
                )
                IconButton(
                    onClick = { /* Copy link */ },
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                }
            }

            IconButton(
                onClick = { context.openUrl(APP_GITHUB_URL) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(painterResource(R.drawable.ic_github_circle_24dp), contentDescription = "GitHub")
            }

            IconButton(
                onClick = onChangelogClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(Icons.Default.History, contentDescription = "Changelog")
            }

            IconButton(
                onClick = { context.openUrl(FAQ_LINK) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(Icons.Default.Help, contentDescription = "FAQ")
            }
        }
    }
}

@Composable
private fun AuthorSectionExpressive(
    onGitHubClick: () -> Unit,
    onEmailClick: () -> Unit,
    onDonateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AboutContributorImage(
                username = "mardous",
                modifier = Modifier.size(72.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PARKER",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Maker",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = stringResource(R.string.mardous_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onGitHubClick) {
                Icon(painterResource(R.drawable.ic_github_circle_24dp), contentDescription = "GitHub")
            }
            IconButton(onClick = onEmailClick) {
                Icon(Icons.Default.Email, contentDescription = "Email")
            }
            if (!App.isPlayStoreBuild()) {
                IconButton(onClick = onDonateClick) {
                    Icon(Icons.Default.VolunteerActivism, contentDescription = "Donate", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AboutSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutListItem(
    index: Int,
    itemCount: Int,
    data: AboutItemData,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    SegmentedListItem(
        onClick = data.onClick,
        shapes = ListItemDefaults.segmentedShapes(index, itemCount),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    data.icon()
                }
            }
        },
        trailingContent = data.trailingContent,
        colors = ListItemDefaults.segmentedColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (data.badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = data.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            if (!data.markdown.isNullOrBlank()) {
                MarkdownText(
                    markdown = data.markdown,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = LocalContentColor.current.copy(alpha = 0.7f),
                    )
                )
            } else if (!data.summary.isNullOrBlank()) {
                Text(
                    text = data.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.7f),
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AboutContributorImage(
    username: String,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = "file:///android_asset/images/${username}.png".toUri(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(CircleShape)
    )
}

@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.anatdx.andronitor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anatdx.andronitor.R
import com.anatdx.andronitor.model.Account
import com.anatdx.andronitor.model.ChangeType
import com.anatdx.andronitor.model.SyncFetchMode
import com.anatdx.andronitor.ui.component.CompactTopBar
import com.anatdx.andronitor.ui.component.EmptyState
import com.anatdx.andronitor.ui.component.ListGroup
import com.anatdx.andronitor.ui.component.ListGroupPadding
import com.anatdx.andronitor.ui.component.ListGroupRow
import com.anatdx.andronitor.ui.component.LoadingScreen
import com.anatdx.andronitor.ui.component.WavyLinearProgress
import com.anatdx.andronitor.ui.component.rememberPinnedScrollBehavior

private val HomeCategories = listOf(
    ChangeType.PROFILE_UPDATE,
    ChangeType.NORMAL_UNFOLLOWED,
    ChangeType.MUTUAL_UNFOLLOWED,
    ChangeType.ONEWAY_UNFOLLOWED,
    ChangeType.TEMPORARILY_RESTRICTED,
    ChangeType.SUSPENDED,
    ChangeType.DEACTIVATED,
    ChangeType.BE_FOLLOWED_BACK,
    ChangeType.NEW_FOLLOWERS_FOLLOWING,
    ChangeType.RECOVERED,
)

@Composable
fun HomeScreen(state: AndronitorUiState, viewModel: AndronitorViewModel) {
    val account = state.activeAccount
    val scrollBehavior = rememberPinnedScrollBehavior()
    var syncMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CompactTopBar(
                title = stringResource(R.string.app_name),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (account != null) {
                SyncFabMenu(
                    expanded = syncMenuOpen,
                    onExpandedChange = { syncMenuOpen = it },
                    onSync = { mode ->
                        syncMenuOpen = false
                        viewModel.runSync(mode)
                    },
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingScreen(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            account == null -> EmptyState(
                icon = Icons.Rounded.AccountCircle,
                title = stringResource(R.string.add_account_first),
                body = stringResource(R.string.account_cookie_explanation),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onAction = { viewModel.selectTab(AppTab.ACCOUNTS) },
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
            ) {
                item { AccountHeroCard(account, state.stats.lastSync) }
                item {
                    StatsRow(
                        following = state.stats.followingCount,
                        followers = state.stats.followersCount,
                    )
                }
                item {
                    // Kept mounted so finishing a sync collapses the card rather
                    // than making it vanish between frames.
                    AnimatedVisibility(visible = state.progress != null && showProgress(state)) {
                        SyncProgressCard(state)
                    }
                }
                item {
                    ListGroup(title = stringResource(R.string.relationship_changes)) {
                        HomeCategories.forEach { type ->
                            row { index, count ->
                                val value = state.stats.counts[type.key] ?: 0
                                val animated by animateIntAsState(value, label = "count:${type.key}")
                                ListGroupRow(
                                    index = index,
                                    count = count,
                                    title = changeTypeTitle(type),
                                    icon = changeTypeIcon(type),
                                    onClick = { viewModel.showCategory(type.key) },
                                    trailing = { CountBadge(animated) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A finished or failed run stays on screen; anything else only shows while busy. */
private fun showProgress(state: AndronitorUiState): Boolean {
    val stage = state.progress?.stage ?: return false
    return state.isBusy || stage == "Completed" || stage == "Failed"
}

@Composable
private fun AccountHeroCard(account: Account, lastSync: Long?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ListGroupPadding, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RemoteAvatar(account.avatarUrl, account.avatarLocalPath, 64.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = account.name ?: stringResource(R.string.unknown),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "@${account.screenName ?: account.id}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = lastSync?.let {
                            stringResource(R.string.last_sync_format, formatDateTime(it))
                        } ?: stringResource(R.string.never_synced),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            account.bio?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StatsRow(following: Int, followers: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ListGroupPadding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(stringResource(R.string.following), following, Icons.Rounded.Group, Modifier.weight(1f))
        StatCard(stringResource(R.string.followers), followers, Icons.Rounded.Group, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: Int, icon: ImageVector, modifier: Modifier = Modifier) {
    val animated by animateIntAsState(value, label = "stat:$label")
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier.padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(animated.toString(), style = MaterialTheme.typography.headlineSmall)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SyncProgressCard(state: AndronitorUiState) {
    val progress = state.progress ?: return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ListGroupPadding, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    localizedProgressText(progress.stage),
                    style = MaterialTheme.typography.titleMedium,
                )
                progress.fraction?.let {
                    Text("${(it * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
                }
            }
            // The pipeline reports stages with no total to divide by, so a null
            // fraction falls through to the indeterminate wavy bar.
            val fraction = progress.fraction
            val animated by animateFloatAsState(fraction ?: 0f, label = "syncProgress")
            WavyLinearProgress(progress = fraction?.let { { animated } })
            if (progress.message.isNotBlank()) {
                Text(
                    localizedProgressText(progress.message),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            state.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (count > 0) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (count > 0) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Box(
            Modifier
                .size(width = 44.dp, height = 32.dp)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

/**
 * Expressive FAB menu: the button morphs into a close affordance and fans the
 * three fetch modes out, so picking a one-off mode no longer means a trip to
 * Settings.
 */
@Composable
private fun SyncFabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSync: (SyncFetchMode) -> Unit,
) {
    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = onExpandedChange,
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.Close else Icons.Rounded.Sync,
                    contentDescription = stringResource(R.string.sync_options),
                    modifier = Modifier.size(28.dp),
                )
            }
        },
    ) {
        SyncFetchMode.entries.forEach { mode ->
            FloatingActionButtonMenuItem(
                onClick = { onSync(mode) },
                icon = { Icon(fetchModeIcon(mode), contentDescription = null) },
                text = { Text(syncModeLabel(mode)) },
            )
        }
    }
}

private fun fetchModeIcon(mode: SyncFetchMode): ImageVector = when (mode) {
    SyncFetchMode.FAST -> Icons.Rounded.Sync
    SyncFetchMode.EXPERT -> Icons.Rounded.Bolt
    SyncFetchMode.FASTEST -> Icons.Rounded.RocketLaunch
}

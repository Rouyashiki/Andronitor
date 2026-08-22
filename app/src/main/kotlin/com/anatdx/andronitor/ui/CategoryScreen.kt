@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.anatdx.andronitor.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anatdx.andronitor.R
import com.anatdx.andronitor.model.ChangeReport
import com.anatdx.andronitor.model.ChangeType
import com.anatdx.andronitor.model.TwitterUser
import com.anatdx.andronitor.ui.component.CollapsingTopBar
import com.anatdx.andronitor.ui.component.EmptyState
import com.anatdx.andronitor.ui.component.ListGroup
import com.anatdx.andronitor.ui.component.ListGroupNavRow
import com.anatdx.andronitor.ui.component.LoadingScreen
import com.anatdx.andronitor.ui.component.rememberCollapsingScrollBehavior

/** The users behind one dashboard category, or a raw followers/following list. */
@Composable
fun CategoryScreen(
    title: String,
    users: List<TwitterUser>,
    loading: Boolean,
    onBack: () -> Unit,
    onUserClick: (TwitterUser) -> Unit,
) {
    val scrollBehavior = rememberCollapsingScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CollapsingTopBar(
                title = title,
                scrollBehavior = scrollBehavior,
                onBack = onBack,
            )
        },
    ) { padding ->
        val content = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            loading -> LoadingScreen(content)
            users.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Group,
                title = stringResource(R.string.no_users),
                body = stringResource(R.string.no_users_body),
                modifier = content,
            )
            else -> LazyColumn(content, contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)) {
                item {
                    ListGroup(title = stringResource(R.string.progress_users_count, users.size)) {
                        users.forEach { user ->
                            row { index, count ->
                                UserGroupRow(index, count, user) { onUserClick(user) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Every recorded change for one user, newest first, each opening a snapshot. */
@Composable
fun UserHistoryScreen(
    user: TwitterUser,
    reports: List<ChangeReport>,
    loading: Boolean,
    onBack: () -> Unit,
    onOpenSnapshot: (ChangeReport) -> Unit,
) {
    val scrollBehavior = rememberCollapsingScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CollapsingTopBar(
                title = "${stringResource(R.string.history)} · @${user.screenName ?: user.restId}",
                scrollBehavior = scrollBehavior,
                onBack = onBack,
            )
        },
    ) { padding ->
        val content = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            loading -> LoadingScreen(content)
            reports.isEmpty() -> EmptyState(
                icon = Icons.Rounded.History,
                title = stringResource(R.string.no_history_found),
                body = stringResource(R.string.open_saved_users_history),
                modifier = content,
            )
            else -> LazyColumn(content, contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)) {
                item {
                    ListGroup {
                        reports.forEach { report ->
                            row { index, count ->
                                val type = ChangeType.fromKey(report.changeType)
                                ListGroupNavRow(
                                    index = index,
                                    count = count,
                                    title = changeTypeTitle(type),
                                    summary = formatDateTime(report.timestamp),
                                    icon = changeTypeIcon(type),
                                    onClick = { onOpenSnapshot(report) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

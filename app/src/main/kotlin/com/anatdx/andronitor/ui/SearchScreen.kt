@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.anatdx.andronitor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anatdx.andronitor.R
import com.anatdx.andronitor.model.AccountStatus
import com.anatdx.andronitor.model.FilterState
import com.anatdx.andronitor.model.SearchField
import com.anatdx.andronitor.model.SearchFilters
import com.anatdx.andronitor.ui.component.CollapsingTopBar
import com.anatdx.andronitor.ui.component.EmptyState
import com.anatdx.andronitor.ui.component.ListGroup
import com.anatdx.andronitor.ui.component.ListGroupItem
import com.anatdx.andronitor.ui.component.ListGroupPadding
import com.anatdx.andronitor.ui.component.ListGroupRow
import com.anatdx.andronitor.ui.component.ListGroupSwitchRow
import com.anatdx.andronitor.ui.component.OptionGroupRow
import com.anatdx.andronitor.ui.component.SearchTopBar
import com.anatdx.andronitor.ui.component.WavyLinearProgress
import com.anatdx.andronitor.ui.component.rememberCollapsingScrollBehavior

@Composable
fun SearchScreen(state: AndronitorUiState, viewModel: AndronitorViewModel) {
    var showFilters by remember { mutableStateOf(false) }
    val scrollBehavior = rememberCollapsingScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SearchTopBar(
                title = stringResource(R.string.nav_search),
                query = state.searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                onSubmit = viewModel::search,
                onClear = { viewModel.updateSearchQuery("") },
                placeholder = stringResource(R.string.search_saved_users),
                scrollBehavior = scrollBehavior,
                actions = {
                    BadgedBox(
                        badge = {
                            val count = state.searchFilters.activeCount
                            if (count > 0) Badge { Text(count.toString()) }
                        },
                    ) {
                        IconButton(onClick = { showFilters = true }) {
                            Icon(
                                Icons.Rounded.Tune,
                                stringResource(R.string.filter),
                                tint = if (state.searchFilters.activeCount > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedVisibility(visible = state.searchRunning) {
                WavyLinearProgress(
                    modifier = Modifier.padding(horizontal = ListGroupPadding, vertical = 8.dp),
                )
            }
            state.searchError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = ListGroupPadding + 14.dp),
                )
            }
            when {
                state.activeAccount == null -> EmptyState(
                    icon = Icons.Rounded.Person,
                    title = stringResource(R.string.add_account_first),
                    body = stringResource(R.string.account_cookie_explanation),
                    modifier = Modifier.fillMaxSize(),
                )
                !state.searchSubmitted -> RecentSearches(
                    history = state.searchHistory,
                    onSelect = viewModel::search,
                    onRemove = viewModel::removeSearchTerm,
                    onClear = viewModel::clearSearchHistory,
                )
                state.searchResults.isEmpty() && !state.searchRunning -> EmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = stringResource(R.string.no_search_results),
                    body = stringResource(R.string.no_search_results_body),
                    modifier = Modifier.fillMaxSize(),
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    item {
                        ListGroup {
                            state.searchResults.forEach { user ->
                                row { index, count ->
                                    UserGroupRow(index, count, user) { viewModel.showUser(user) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        SearchFiltersDialog(
            initial = state.searchFilters,
            onDismiss = { showFilters = false },
            onApply = { filters ->
                viewModel.updateSearchFilters(filters)
                showFilters = false
            },
        )
    }
}

@Composable
private fun RecentSearches(
    history: List<String>,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (history.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.History,
            title = stringResource(R.string.no_recent_searches),
            body = stringResource(R.string.no_recent_searches_body),
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            ListGroup(title = stringResource(R.string.recent_searches)) {
                history.forEach { term ->
                    row { index, count ->
                        ListGroupRow(
                            index = index,
                            count = count,
                            title = term,
                            icon = Icons.Rounded.History,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { onSelect(term) },
                            trailing = {
                                IconButton(onClick = { onRemove(term) }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        stringResource(R.string.clear),
                                        Modifier.size(18.dp),
                                    )
                                }
                            },
                        )
                    }
                }
                row { index, count ->
                    ListGroupRow(
                        index = index,
                        count = count,
                        title = stringResource(R.string.clear_search_history),
                        icon = Icons.Rounded.Delete,
                        iconTint = MaterialTheme.colorScheme.error,
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = onClear,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchFiltersDialog(
    initial: SearchFilters,
    onDismiss: () -> Unit,
    onApply: (SearchFilters) -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    val scrollBehavior = rememberCollapsingScrollBehavior()
    // Full-screen and edge-to-edge, otherwise the scrim shows through as grey
    // strips behind the status and navigation bars.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BackHandler(onBack = onDismiss)
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    CollapsingTopBar(
                        title = stringResource(R.string.filter),
                        scrollBehavior = scrollBehavior,
                        onBack = onDismiss,
                        actions = {
                            IconButton(onClick = { draft = SearchFilters() }) {
                                Icon(Icons.Rounded.RestartAlt, stringResource(R.string.reset))
                            }
                        },
                    )
                },
                bottomBar = {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Button(
                            onClick = { onApply(draft) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(horizontal = ListGroupPadding, vertical = 16.dp),
                        ) {
                            Icon(Icons.Rounded.Done, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.apply))
                        }
                    }
                },
            ) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    ListGroup(title = stringResource(R.string.search_fields)) {
                        row { index, count ->
                            ListGroupSwitchRow(
                                index = index,
                                count = count,
                                title = stringResource(R.string.enable_restid_searching),
                                summary = stringResource(R.string.enable_restid_searching_subtitle),
                                checked = SearchField.REST_ID in draft.fields,
                                onCheckedChange = {
                                    draft = draft.copy(fields = draft.fields.toggle(SearchField.REST_ID, it))
                                },
                            )
                        }
                        row { index, count ->
                            ListGroupItem(index, count) {
                                FlowRow(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    SearchField.entries
                                        .filter { it != SearchField.REST_ID }
                                        .forEach { field ->
                                            val selected = field in draft.fields
                                            FilterChip(
                                                selected = selected,
                                                onClick = {
                                                    draft = draft.copy(
                                                        fields = draft.fields.toggle(field, !selected),
                                                    )
                                                },
                                                label = { Text(searchFieldLabel(field)) },
                                                leadingIcon = if (selected) {
                                                    { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }
                                                } else {
                                                    null
                                                },
                                            )
                                        }
                                }
                            }
                        }
                    }

                    ListGroup(title = stringResource(R.string.relationship)) {
                        row { index, count ->
                            TriStateRow(index, count, stringResource(R.string.followers), draft.isFollower) {
                                draft = draft.copy(isFollower = it)
                            }
                        }
                        row { index, count ->
                            TriStateRow(index, count, stringResource(R.string.following), draft.isFollowing) {
                                draft = draft.copy(isFollowing = it)
                            }
                        }
                    }

                    ListGroup(title = stringResource(R.string.attributes)) {
                        row { index, count ->
                            TriStateRow(index, count, stringResource(R.string.protected_account), draft.isProtected) {
                                draft = draft.copy(isProtected = it)
                            }
                        }
                        row { index, count ->
                            TriStateRow(index, count, stringResource(R.string.verified), draft.isVerified) {
                                draft = draft.copy(isVerified = it)
                            }
                        }
                    }

                    ListGroup(title = stringResource(R.string.account_status)) {
                        row { index, count ->
                            ListGroupItem(index, count) {
                                FlowRow(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    AccountStatus.entries.forEach { status ->
                                        val selected = status in draft.statuses
                                        FilterChip(
                                            selected = selected,
                                            onClick = {
                                                draft = draft.copy(
                                                    statuses = draft.statuses.toggle(status, !selected),
                                                )
                                            },
                                            label = { Text(accountStatusLabel(status)) },
                                            leadingIcon = if (selected) {
                                                { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }
                                            } else {
                                                null
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TriStateRow(
    index: Int,
    count: Int,
    label: String,
    value: FilterState,
    onChange: (FilterState) -> Unit,
) {
    OptionGroupRow(
        index = index,
        count = count,
        title = label,
        options = FilterState.entries,
        selected = value,
        onSelect = onChange,
        label = { filterStateLabel(it) },
    )
}

private fun <T> Set<T>.toggle(value: T, enabled: Boolean): Set<T> =
    if (enabled) this + value else this - value

@Composable
private fun searchFieldLabel(field: SearchField): String = stringResource(
    when (field) {
        SearchField.REST_ID -> R.string.field_rest_id
        SearchField.SCREEN_NAME -> R.string.field_screen_name
        SearchField.NAME -> R.string.field_name
        SearchField.BIO -> R.string.field_bio
        SearchField.LOCATION -> R.string.field_location
        SearchField.LINK -> R.string.field_link
    },
)

@Composable
private fun accountStatusLabel(status: AccountStatus): String = stringResource(
    when (status) {
        AccountStatus.NORMAL -> R.string.status_normal
        AccountStatus.SUSPENDED -> R.string.change_suspended
        AccountStatus.DEACTIVATED -> R.string.change_deactivated
        AccountStatus.TEMPORARILY_RESTRICTED -> R.string.change_temporarily_restricted
    },
)

@Composable
private fun filterStateLabel(state: FilterState): String = stringResource(
    when (state) {
        FilterState.ALL -> R.string.filter_all
        FilterState.YES -> R.string.filter_yes
        FilterState.NO -> R.string.filter_no
    },
)

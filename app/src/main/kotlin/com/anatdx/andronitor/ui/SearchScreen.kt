@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.anatdx.andronitor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anatdx.andronitor.R
import com.anatdx.andronitor.model.AccountStatus
import com.anatdx.andronitor.model.FilterState
import com.anatdx.andronitor.model.SearchField
import com.anatdx.andronitor.model.SearchFilters

@Composable
fun SearchScreen(state: AndronitorUiState, viewModel: AndronitorViewModel) {
    var showFilters by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_search)) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.search_saved_users)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Outlined.Clear, stringResource(R.string.clear))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        viewModel.search()
                    }),
                )
                Spacer(Modifier.size(8.dp))
                BadgedBox(
                    badge = {
                        val count = state.searchFilters.activeCount
                        if (count > 0) Badge { Text(count.toString()) }
                    },
                ) {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(
                            Icons.Outlined.Tune,
                            stringResource(R.string.filter),
                            tint = if (state.searchFilters.activeCount > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (state.searchRunning) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.searchError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }
            when {
                state.activeAccount == null -> CenteredHint(stringResource(R.string.add_account_first))
                !state.searchSubmitted -> RecentSearches(
                    history = state.searchHistory,
                    onSelect = { term -> focusManager.clearFocus(); viewModel.search(term) },
                    onRemove = viewModel::removeSearchTerm,
                    onClear = viewModel::clearSearchHistory,
                )
                state.searchResults.isEmpty() && !state.searchRunning ->
                    CenteredHint(stringResource(R.string.no_search_results))
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.searchResults, key = { it.restId }) { user ->
                        UserRow(user) { viewModel.showUser(user) }
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
private fun CenteredHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.History,
                null,
                Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.no_recent_searches),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                stringResource(R.string.recent_searches),
                Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        items(history, key = { it }) { term ->
            ListItem(
                leadingContent = { Icon(Icons.Outlined.History, null) },
                headlineContent = { Text(term) },
                trailingContent = {
                    IconButton(onClick = { onRemove(term) }) {
                        Icon(Icons.Filled.Close, stringResource(R.string.clear), Modifier.size(18.dp))
                    }
                },
                modifier = Modifier.clickable { onSelect(term) },
            )
        }
        item {
            HorizontalDivider()
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onClear) { Text(stringResource(R.string.clear_search_history)) }
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
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BackHandler(onBack = onDismiss)
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.filter)) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, stringResource(R.string.cancel))
                            }
                        },
                        actions = {
                            IconButton(onClick = { draft = SearchFilters() }) {
                                Icon(Icons.Outlined.RestartAlt, stringResource(R.string.reset))
                            }
                        },
                    )
                },
                bottomBar = {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                        FilledTonalButton(
                            onClick = { onApply(draft) },
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        ) {
                            Icon(Icons.Filled.Done, null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.apply))
                        }
                    }
                },
            ) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    FilterSection(stringResource(R.string.search_fields), Icons.Outlined.ManageSearch) {
                        ListItem(
                            leadingContent = { Icon(Icons.Outlined.Search, null) },
                            headlineContent = { Text(stringResource(R.string.enable_restid_searching)) },
                            supportingContent = { Text(stringResource(R.string.enable_restid_searching_subtitle)) },
                            trailingContent = {
                                Switch(
                                    checked = SearchField.REST_ID in draft.fields,
                                    onCheckedChange = { draft = draft.copy(fields = draft.fields.toggle(SearchField.REST_ID, it)) },
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SearchField.entries.filter { it != SearchField.REST_ID }.forEach { field ->
                                val selected = field in draft.fields
                                FilterChip(
                                    selected = selected,
                                    onClick = { draft = draft.copy(fields = draft.fields.toggle(field, !selected)) },
                                    label = { Text(searchFieldLabel(field)) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }
                                    } else null,
                                )
                            }
                        }
                    }
                    FilterSection(stringResource(R.string.relationship), Icons.Outlined.Person) {
                        TriStateRow(stringResource(R.string.followers), Icons.Outlined.PersonOutline, draft.isFollower) {
                            draft = draft.copy(isFollower = it)
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        TriStateRow(stringResource(R.string.following), Icons.Outlined.Person, draft.isFollowing) {
                            draft = draft.copy(isFollowing = it)
                        }
                    }
                    FilterSection(stringResource(R.string.attributes), Icons.Outlined.Verified) {
                        TriStateRow(stringResource(R.string.protected_account), Icons.Outlined.Lock, draft.isProtected) {
                            draft = draft.copy(isProtected = it)
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        TriStateRow(stringResource(R.string.verified), Icons.Outlined.Verified, draft.isVerified) {
                            draft = draft.copy(isVerified = it)
                        }
                    }
                    FilterSection(stringResource(R.string.account_status), Icons.Outlined.AccountCircle) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccountStatus.entries.forEach { status ->
                                val selected = status in draft.statuses
                                FilterChip(
                                    selected = selected,
                                    onClick = { draft = draft.copy(statuses = draft.statuses.toggle(status, !selected)) },
                                    label = { Text(accountStatusLabel(status)) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }
                                    } else null,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun FilterSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun TriStateRow(label: String, icon: ImageVector, value: FilterState, onChange: (FilterState) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(8.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        SingleChoiceSegmentedButtonRow {
            FilterState.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = value == entry,
                    onClick = { onChange(entry) },
                    shape = SegmentedButtonDefaults.itemShape(index, FilterState.entries.size),
                ) { Text(filterStateLabel(entry)) }
            }
        }
    }
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

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.anatdx.andronitor.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anatdx.andronitor.AppLocale
import com.anatdx.andronitor.BrowserLoginActivity
import com.anatdx.andronitor.R
import com.anatdx.andronitor.model.Account
import com.anatdx.andronitor.model.AppSettings
import com.anatdx.andronitor.model.ChangeType
import com.anatdx.andronitor.model.MediaSaveScope
import com.anatdx.andronitor.model.SyncFetchMode
import com.anatdx.andronitor.model.TwitterUser
import com.anatdx.andronitor.ui.theme.AndronitorTheme
import com.anatdx.andronitor.xposed.DiscoveredXAccount
import com.anatdx.andronitor.xposed.XposedServiceConnection

@Composable
fun AndronitorApp(viewModel: AndronitorViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    AndronitorTheme(darkTheme = state.settings.darkTheme || isSystemInDarkTheme()) {
        when {
            state.selectedUser != null -> {
                BackHandler { viewModel.closeUser() }
                ProfileScreen(state.selectedUser!!, viewModel::closeUser)
            }
            state.selectedCategory != null -> {
                BackHandler { viewModel.clearCategory() }
                CategoryScreen(
                    title = categoryTitle(state.selectedCategory),
                    users = state.users,
                    loading = state.isLoading,
                    onBack = viewModel::clearCategory,
                    onUserClick = viewModel::showUser,
                )
            }
            else -> MainScaffold(state, viewModel)
        }
    }
}

@Composable
private fun MainScaffold(state: AndronitorUiState, viewModel: AndronitorViewModel) {
    Scaffold(
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets.navigationBars) {
                listOf(
                    AppTab.HOME to Icons.Outlined.Home,
                    AppTab.SEARCH to Icons.Outlined.Search,
                    AppTab.ACCOUNTS to Icons.Outlined.People,
                    AppTab.SETTINGS to Icons.Outlined.Settings,
                ).forEach { (tab, icon) ->
                    val label = tabLabel(tab)
                    NavigationBarItem(
                        selected = state.tab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).windowInsetsPadding(WindowInsets.safeDrawing)) {
            when (state.tab) {
                AppTab.HOME -> HomeScreen(state, viewModel)
                AppTab.SEARCH -> SearchScreen(state, viewModel)
                AppTab.ACCOUNTS -> AccountsScreen(state, viewModel)
                AppTab.SETTINGS -> SettingsScreen(state, viewModel)
            }
        }
    }
}

@Composable
private fun HomeScreen(state: AndronitorUiState, viewModel: AndronitorViewModel) {
    val account = state.activeAccount
    Scaffold(
        topBar = { TopAppBar(title = { Text("Andronitor") }) },
        floatingActionButton = {
            if (account != null) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.runSync() },
                    icon = { Icon(Icons.Outlined.Sync, contentDescription = null) },
                    text = { Text(stringResource(R.string.run_sync)) },
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(32.dp))
            }
            account == null -> EmptyState(
                modifier = Modifier.fillMaxSize().padding(padding),
                icon = Icons.Outlined.AccountCircle,
                title = stringResource(R.string.add_account_first),
                body = stringResource(R.string.account_cookie_explanation),
                action = { viewModel.selectTab(AppTab.ACCOUNTS) },
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { AccountSummaryCard(account) }
                item { StatsRow(state.stats.followersCount, state.stats.followingCount) }
                state.progress?.let { progress ->
                    if (state.isBusy || progress.stage == "Completed" || progress.stage == "Failed") {
                        item { SyncProgressCard(state) }
                    }
                }
                item { Text(stringResource(R.string.relationship_changes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
                val categories = listOf(
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
                items(categories) { type -> ChangeRow(type, state.stats.counts[type.key] ?: 0) { viewModel.showCategory(type.key) } }
            }
        }
    }
}

@Composable
private fun AccountSummaryCard(account: Account) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RemoteAvatar(account.avatarUrl, account.avatarLocalPath, 56.dp)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(account.name ?: stringResource(R.string.unknown), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("@${account.screenName ?: account.id}", color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.account_id_format, account.id), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            account.bio?.takeIf { it.isNotBlank() }?.let {
                Text(it, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun StatsRow(followers: Int, following: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(stringResource(R.string.following), following, Modifier.weight(1f))
        StatCard(stringResource(R.string.followers), followers, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SyncProgressCard(state: AndronitorUiState) {
    val progress = state.progress ?: return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(localizedProgressText(progress.stage), fontWeight = FontWeight.SemiBold)
                progress.fraction?.let { Text("${(it * 100).toInt()}%") }
            }
            LinearProgressIndicator(progress = { progress.fraction ?: 0f }, modifier = Modifier.fillMaxWidth())
            if (progress.message.isNotBlank()) Text(localizedProgressText(progress.message), style = MaterialTheme.typography.bodySmall)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun ChangeRow(type: ChangeType, count: Int, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        ListItem(
            leadingContent = { Icon(Icons.Outlined.Badge, contentDescription = null) },
            headlineContent = { Text(changeTypeTitle(type)) },
            supportingContent = { Text(stringResource(R.string.open_saved_users_history)) },
            trailingContent = { Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        )
    }
}

@Composable
private fun AccountsScreen(state: AndronitorUiState, viewModel: AndronitorViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var editAccount by remember { mutableStateOf<Account?>(null) }
    val context = LocalContext.current
    val browserLoginLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra(BrowserLoginActivity.EXTRA_COOKIE)
                ?.takeIf { it.isNotBlank() }
                ?.let(viewModel::addAccount)
        }
    }

    // Surface cookies imported from the X app (delivered by the Xposed hook).
    LaunchedEffect(state.info) {
        state.info?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearInfo()
        }
    }

    fun openXApp() {
        // On modern LSPosed, proactively ask the framework to scope this module to
        // the X app so the hook actually loads there. No-op on classic frameworks.
        XposedServiceConnection.requestXScope()
        val launch = context.packageManager.getLaunchIntentForPackage("com.twitter.android")
        if (launch != null) {
            context.startActivity(launch)
        } else {
            Toast.makeText(context, context.getString(R.string.x_app_not_installed), Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_accounts)) },
                actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Outlined.Add, stringResource(R.string.add_account)) } },
            )
        },
    ) { padding ->
        if (state.accounts.isEmpty()) {
            EmptyState(Modifier.fillMaxSize().padding(padding), Icons.Outlined.People, stringResource(R.string.no_accounts), stringResource(R.string.no_accounts_body)) { showAdd = true }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.accounts, key = { it.id }) { account ->
                    AccountCard(account, account.id == state.activeAccount?.id, onSelect = { viewModel.selectAccount(account) }, onEdit = { editAccount = account }, onDelete = { viewModel.deleteAccount(account) })
                }
            }
        }
    }
    if (showAdd) {
        CookieDialog(
            title = stringResource(R.string.add_account),
            hint = stringResource(R.string.paste_cookie_hint),
            onDismiss = { showAdd = false },
            onBrowserLogin = {
                showAdd = false
                browserLoginLauncher.launch(Intent(context, BrowserLoginActivity::class.java))
            },
            onImportFromX = {
                // Keep the dialog open: the user goes to X, switches accounts, and
                // returns to watch each account flip to "imported" live.
                openXApp()
            },
            discoveredAccounts = state.discoveredAccounts,
            importedIds = state.accounts.map { it.id }.toSet(),
            onConfirm = { cookie ->
                viewModel.addAccount(cookie)
                showAdd = false
            },
        )
    }
    editAccount?.let { account ->
        CookieDialog(
            title = stringResource(R.string.update_cookie),
            hint = stringResource(R.string.cookie_account_hint, account.id),
            onDismiss = { editAccount = null },
            onConfirm = { cookie ->
                viewModel.updateCookie(account, cookie)
                editAccount = null
            },
        )
    }
}

@Composable
private fun AccountCard(account: Account, selected: Boolean, onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onSelect, colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteAvatar(account.avatarUrl, account.avatarLocalPath)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(account.name ?: stringResource(R.string.unknown), fontWeight = FontWeight.Bold)
                Text("@${account.screenName ?: account.id}", color = MaterialTheme.colorScheme.primary)
                if (selected) Text(stringResource(R.string.active_account), style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Tune, stringResource(R.string.edit_cookie)) }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, stringResource(R.string.delete_account), tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun CookieDialog(
    title: String,
    hint: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onBrowserLogin: (() -> Unit)? = null,
    onImportFromX: (() -> Unit)? = null,
    discoveredAccounts: List<DiscoveredXAccount> = emptyList(),
    importedIds: Set<String> = emptySet(),
) {
    var cookie by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (onImportFromX != null) {
                    Text(
                        stringResource(R.string.import_from_x_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = onImportFromX,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    ) { Text(stringResource(R.string.import_from_x)) }
                    if (discoveredAccounts.isNotEmpty()) {
                        Text(
                            stringResource(R.string.x_accounts_detected, discoveredAccounts.size),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        discoveredAccounts.forEach { acc ->
                            val imported = acc.id.isNotEmpty() && acc.id in importedIds
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (acc.screenName.isNotEmpty()) "@${acc.screenName}" else acc.id,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    stringResource(if (imported) R.string.x_account_imported else R.string.x_account_pending),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (imported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            stringResource(R.string.x_accounts_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
                OutlinedTextField(cookie, { cookie = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text(stringResource(R.string.cookie)) }, placeholder = { Text(hint) })
            }
        },
        confirmButton = { Button(onClick = { onConfirm(cookie) }, enabled = cookie.isNotBlank()) { Text(stringResource(R.string.save)) } },
        dismissButton = {
            Row {
                if (onBrowserLogin != null) {
                    TextButton(onClick = onBrowserLogin) { Text(stringResource(R.string.browser_login)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Composable
private fun SearchScreen(state: AndronitorUiState, viewModel: AndronitorViewModel) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_search)) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(state.searchQuery, viewModel::updateSearchQuery, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.search_x_users)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) })
                Spacer(Modifier.size(8.dp))
                FilledTonalButton(onClick = viewModel::search, enabled = !state.isBusy && state.searchQuery.isNotBlank()) { Text(stringResource(R.string.nav_search)) }
            }
            if (state.isBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(state.searchResults, key = { it.restId }) { user -> UserRow(user) { viewModel.showUser(user) } }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}

@Composable
private fun SettingsScreen(state: AndronitorUiState, viewModel: AndronitorViewModel) {
    var modeMenuOpen by remember { mutableStateOf(false) }
    var languageMenuOpen by remember { mutableStateOf(false) }
    val settings = state.settings
    val context = LocalContext.current
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.writeBackup(uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.restoreBackup(uri)
    }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.sync), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            SettingSwitch(stringResource(R.string.dark_theme), settings.darkTheme) { viewModel.updateSettings(settings.copy(darkTheme = it)) }
            Box {
                OutlinedButton(onClick = { languageMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.language_format, languageLabel(settings.language)))
                }
                DropdownMenu(expanded = languageMenuOpen, onDismissRequest = { languageMenuOpen = false }) {
                    listOf(
                        AppLocale.LANGUAGE_SYSTEM,
                        AppLocale.LANGUAGE_SIMPLIFIED_CHINESE,
                        AppLocale.LANGUAGE_ENGLISH,
                    ).forEach { language ->
                        DropdownMenuItem(
                            text = { Text(languageLabel(language)) },
                            onClick = {
                                viewModel.updateSettings(settings.copy(language = language))
                                languageMenuOpen = false
                                context.findActivity()?.recreate()
                            },
                        )
                    }
                }
            }
            Box {
                OutlinedButton(onClick = { modeMenuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.fetch_mode_format, syncModeLabel(settings.syncFetchMode))) }
                DropdownMenu(expanded = modeMenuOpen, onDismissRequest = { modeMenuOpen = false }) {
                    SyncFetchMode.entries.forEach { mode -> DropdownMenuItem(text = { Text(syncModeLabel(mode)) }, onClick = { viewModel.updateSettings(settings.copy(syncFetchMode = mode)); modeMenuOpen = false }) }
                }
            }
            SettingSwitch(stringResource(R.string.save_avatar_history), settings.saveAvatarHistory) { viewModel.updateSettings(settings.copy(saveAvatarHistory = it)) }
            SettingSwitch(stringResource(R.string.save_banner_history), settings.saveBannerHistory) { viewModel.updateSettings(settings.copy(saveBannerHistory = it)) }
            SettingSwitch(stringResource(R.string.use_high_quality_avatars), settings.avatarQualityHigh) { viewModel.updateSettings(settings.copy(avatarQualityHigh = it)) }
            Text(stringResource(R.string.media_scope), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MediaSaveScope.entries.forEach { scope ->
                    AssistChip(onClick = { viewModel.updateSettings(settings.copy(mediaSaveScope = scope)) }, label = { Text(mediaScopeLabel(scope)) })
                }
            }
            HorizontalDivider()
            Text(stringResource(R.string.http_requests), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            SettingSwitch(stringResource(R.string.use_curl_cffi_proxy), settings.apiRequestMode == "curl_cffi") { viewModel.updateSettings(settings.copy(apiRequestMode = if (it) "curl_cffi" else "dio")) }
            if (settings.apiRequestMode == "curl_cffi") {
                var url by remember(settings.remoteFastApiUrl) { mutableStateOf(settings.remoteFastApiUrl) }
                var key by remember(settings.fastApiApiKey) { mutableStateOf(settings.fastApiApiKey) }
                OutlinedTextField(url, { url = it; viewModel.updateSettings(settings.copy(remoteFastApiUrl = it)) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.fastapi_url)) })
                OutlinedTextField(key, { key = it; viewModel.updateSettings(settings.copy(fastApiApiKey = it)) }, Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), label = { Text(stringResource(R.string.fastapi_api_key)) })
            }
            OutlinedButton(onClick = viewModel::refreshQueryIds, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.refresh_query_ids)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { backupLauncher.launch("andronitor_backup.zip") }, enabled = !state.isBusy, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.backup)) }
                OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/zip")) }, enabled = !state.isBusy, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.restore)) }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text("Andronitor 1.0.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(headlineContent = { Text(label) }, trailingContent = { Switch(checked, onCheckedChange) })
}

@Composable
private fun CategoryScreen(title: String, users: List<TwitterUser>, loading: Boolean, onBack: () -> Unit, onUserClick: (TwitterUser) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium) } }) }) { padding ->
        when {
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(padding))
            users.isEmpty() -> EmptyState(Modifier.fillMaxSize().padding(padding), Icons.Outlined.People, stringResource(R.string.no_users), stringResource(R.string.no_users_body))
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(users, key = { it.restId }) { user -> UserRow(user) { onUserClick(user) } } }
        }
    }
}

@Composable
private fun UserRow(user: TwitterUser, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteAvatar(user.avatarUrl, user.avatarLocalPath, 48.dp)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(user.name ?: stringResource(R.string.unknown), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("@${user.screenName ?: user.restId}", color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                user.bio?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (user.isFollower) Text(stringResource(R.string.follower), style = MaterialTheme.typography.labelSmall)
                if (user.isFollowing) Text(stringResource(R.string.following), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ProfileScreen(user: TwitterUser, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(user.name ?: stringResource(R.string.profile)) }, navigationIcon = { IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RemoteAvatar(user.avatarUrl, user.avatarLocalPath, 72.dp)
                        Spacer(Modifier.size(14.dp))
                        Column {
                            Text(user.name ?: stringResource(R.string.unknown), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("@${user.screenName ?: user.restId}", color = MaterialTheme.colorScheme.primary)
                            if (user.isVerified) Text(stringResource(R.string.verified), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    user.bio?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    user.location?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            StatsRow(user.followersCount, user.followingCount)
            Text(stringResource(R.string.user_id_format, user.restId), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (user.status != "normal") Text(localizedStatus(user.status), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier, icon: ImageVector, title: String, body: String, action: (() -> Unit)? = null) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) { Spacer(Modifier.height(20.dp)); Button(onClick = action) { Text(stringResource(R.string.continue_action)) } }
    }
}

@Composable
private fun tabLabel(tab: AppTab): String = when (tab) {
    AppTab.HOME -> stringResource(R.string.nav_home)
    AppTab.SEARCH -> stringResource(R.string.nav_search)
    AppTab.ACCOUNTS -> stringResource(R.string.nav_accounts)
    AppTab.SETTINGS -> stringResource(R.string.nav_settings)
}

@Composable
private fun categoryTitle(key: String?): String = when (key) {
    "followers" -> stringResource(R.string.followers)
    "following" -> stringResource(R.string.following)
    else -> changeTypeTitle(ChangeType.fromKey(key.orEmpty()))
}

@Composable
private fun changeTypeTitle(type: ChangeType): String = stringResource(when (type) {
    ChangeType.PROFILE_UPDATE -> R.string.change_profile_updates
    ChangeType.NORMAL_UNFOLLOWED -> R.string.change_unfollowed
    ChangeType.MUTUAL_UNFOLLOWED -> R.string.change_mutual_unfollowed
    ChangeType.ONEWAY_UNFOLLOWED -> R.string.change_oneway_unfollowed
    ChangeType.TEMPORARILY_RESTRICTED -> R.string.change_temporarily_restricted
    ChangeType.SUSPENDED -> R.string.change_suspended
    ChangeType.DEACTIVATED -> R.string.change_deactivated
    ChangeType.BE_FOLLOWED_BACK -> R.string.change_followed_back
    ChangeType.NEW_FOLLOWERS_FOLLOWING -> R.string.change_new_followers_following
    ChangeType.RECOVERED -> R.string.change_recovered
    ChangeType.OTHER_REASONS -> R.string.change_other_reasons
})

@Composable
private fun syncModeLabel(mode: SyncFetchMode): String = stringResource(when (mode) {
    SyncFetchMode.FAST -> R.string.mode_fast
    SyncFetchMode.EXPERT -> R.string.mode_expert
    SyncFetchMode.FASTEST -> R.string.mode_fastest
})

@Composable
private fun languageLabel(language: String): String = stringResource(when (language) {
    AppLocale.LANGUAGE_ENGLISH -> R.string.language_english
    AppLocale.LANGUAGE_SIMPLIFIED_CHINESE -> R.string.language_simplified_chinese
    else -> R.string.language_system
})

@Composable
private fun mediaScopeLabel(scope: MediaSaveScope): String = stringResource(when (scope) {
    MediaSaveScope.ALL -> R.string.scope_all
    MediaSaveScope.FOLLOWING_ONLY -> R.string.scope_following_only
    MediaSaveScope.MUTUAL_ONLY -> R.string.scope_mutual_only
})

@Composable
private fun localizedProgressText(text: String): String {
    val resource = when (text) {
        "Preparing" -> R.string.progress_preparing
        "Refreshing profile" -> R.string.progress_refreshing_profile
        "Fetching followers" -> R.string.progress_fetching_followers
        "Fetching following" -> R.string.progress_fetching_following
        "Analyzing changes" -> R.string.progress_analyzing_changes
        "Downloading media" -> R.string.progress_downloading_media
        "Completed" -> R.string.progress_completed
        "Failed" -> R.string.progress_failed
        "Starting" -> R.string.progress_starting
        "Comparing local history" -> R.string.progress_comparing_history
        "Checking saved media" -> R.string.progress_checking_media
        "Sync finished" -> R.string.progress_sync_finished
        "Sync failed" -> R.string.progress_sync_failed
        else -> null
    }
    if (resource != null) return stringResource(resource)
    val userCount = text.removeSuffix(" users").toIntOrNull()
    return if (userCount != null && text.endsWith(" users")) stringResource(R.string.progress_users_count, userCount) else text
}

@Composable
private fun localizedStatus(status: String): String = when (status) {
    "suspended" -> stringResource(R.string.change_suspended)
    "deactivated" -> stringResource(R.string.change_deactivated)
    "recovered" -> stringResource(R.string.change_recovered)
    "temporarily_restricted" -> stringResource(R.string.change_temporarily_restricted)
    else -> status
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.anatdx.andronitor.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anatdx.andronitor.BrowserLoginActivity
import com.anatdx.andronitor.R
import com.anatdx.andronitor.model.Account
import com.anatdx.andronitor.ui.component.AndroAlertDialog
import com.anatdx.andronitor.ui.component.CollapsingTopBar
import com.anatdx.andronitor.ui.component.EmptyState
import com.anatdx.andronitor.ui.component.ListGroup
import com.anatdx.andronitor.ui.component.ListGroupItem
import com.anatdx.andronitor.ui.component.ListGroupRow
import com.anatdx.andronitor.ui.component.RoundedAreaTextField
import com.anatdx.andronitor.ui.component.rememberCollapsingScrollBehavior
import com.anatdx.andronitor.xposed.DiscoveredXAccount
import com.anatdx.andronitor.xposed.XposedServiceConnection

@Composable
fun AccountsScreen(state: AndronitorUiState, viewModel: AndronitorViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var editAccount by remember { mutableStateOf<Account?>(null) }
    val context = LocalContext.current
    val scrollBehavior = rememberCollapsingScrollBehavior()
    val xAppMissingMessage = stringResource(R.string.x_app_not_installed)
    val browserLoginLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
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
            Toast.makeText(context, xAppMissingMessage, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CollapsingTopBar(
                title = stringResource(R.string.nav_accounts),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_account)) },
            )
        },
    ) { padding ->
        if (state.accounts.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Group,
                title = stringResource(R.string.no_accounts),
                body = stringResource(R.string.no_accounts_body),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onAction = { showAdd = true },
                actionLabel = stringResource(R.string.add_account),
            )
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
            ) {
                item {
                    ListGroup {
                        state.accounts.forEach { account ->
                            row { index, count ->
                                AccountRow(
                                    index = index,
                                    count = count,
                                    account = account,
                                    selected = account.id == state.activeAccount?.id,
                                    onSelect = { viewModel.selectAccount(account) },
                                    onEdit = { editAccount = account },
                                    onDelete = { viewModel.deleteAccount(account) },
                                )
                            }
                        }
                    }
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
            // Keep the dialog open: the user goes to X, switches accounts, and
            // returns to watch each account flip to "imported" live.
            onImportFromX = { openXApp() },
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
private fun AccountRow(
    index: Int,
    count: Int,
    account: Account,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ListGroupItem(
        index = index,
        count = count,
        containerColor = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        onClick = onSelect,
        contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
    ) {
        RemoteAvatar(account.avatarUrl, account.avatarLocalPath, 48.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = account.name ?: stringResource(R.string.unknown),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${account.screenName ?: account.id}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (selected) {
                Text(
                    text = stringResource(R.string.active_account),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Rounded.Tune, stringResource(R.string.edit_cookie))
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.Delete,
                stringResource(R.string.delete_account),
                tint = MaterialTheme.colorScheme.error,
            )
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
    AndroAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (onImportFromX != null) {
                    Text(
                        stringResource(R.string.import_from_x_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = onImportFromX,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.import_from_x))
                    }
                    if (discoveredAccounts.isNotEmpty()) {
                        DiscoveredAccountList(discoveredAccounts, importedIds)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                }
                RoundedAreaTextField(
                    value = cookie,
                    onValueChange = { cookie = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.cookie),
                    placeholder = hint,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(cookie) }, enabled = cookie.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
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
private fun DiscoveredAccountList(accounts: List<DiscoveredXAccount>, importedIds: Set<String>) {
    Text(
        stringResource(R.string.x_accounts_detected, accounts.size),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
    )
    accounts.forEachIndexed { index, account ->
        val imported = account.id.isNotEmpty() && account.id in importedIds
        ListGroupRow(
            index = index,
            count = accounts.size,
            title = account.screenName.takeIf { it.isNotEmpty() }?.let { "@$it" } ?: account.id,
            summary = stringResource(
                if (imported) R.string.x_account_imported else R.string.x_account_pending,
            ),
            icon = if (imported) Icons.Rounded.CheckCircle else Icons.Rounded.Language,
            iconTint = if (imported) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
    Text(
        stringResource(R.string.x_accounts_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
    )
}

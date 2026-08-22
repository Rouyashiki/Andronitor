@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.anatdx.andronitor.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.anatdx.andronitor.AppLocale
import com.anatdx.andronitor.BuildConfig
import com.anatdx.andronitor.R
import com.anatdx.andronitor.model.MediaSaveScope
import com.anatdx.andronitor.model.SyncFetchMode
import com.anatdx.andronitor.model.ThemeMode
import com.anatdx.andronitor.ui.component.AndroAlertDialog
import com.anatdx.andronitor.ui.component.CollapsingTopBar
import com.anatdx.andronitor.ui.component.ListGroup
import com.anatdx.andronitor.ui.component.ListGroupItem
import com.anatdx.andronitor.ui.component.ListGroupNavRow
import com.anatdx.andronitor.ui.component.ListGroupPadding
import com.anatdx.andronitor.ui.component.ListGroupRow
import com.anatdx.andronitor.ui.component.ListGroupSwitchRow
import com.anatdx.andronitor.ui.component.OptionGroupRow
import com.anatdx.andronitor.ui.component.RoundTextField
import com.anatdx.andronitor.ui.component.rememberCollapsingScrollBehavior

private const val CURL_CFFI = "curl_cffi"
private const val DIO = "dio"

@Composable
fun SettingsScreen(state: AndronitorUiState, viewModel: AndronitorViewModel) {
    val settings = state.settings
    val context = LocalContext.current
    val scrollBehavior = rememberCollapsingScrollBehavior()
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> if (uri != null) viewModel.writeBackup(uri) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.restoreBackup(uri) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CollapsingTopBar(
                title = stringResource(R.string.nav_settings),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            ListGroup(title = stringResource(R.string.appearance)) {
                row { index, count ->
                    OptionGroupRow(
                        index = index,
                        count = count,
                        title = stringResource(R.string.theme_mode),
                        icon = Icons.Rounded.Palette,
                        options = ThemeMode.entries,
                        selected = settings.themeMode,
                        onSelect = { viewModel.updateSettings(settings.copy(themeMode = it)) },
                        label = { themeModeLabel(it) },
                    )
                }
                row { index, count ->
                    ListGroupSwitchRow(
                        index = index,
                        count = count,
                        title = stringResource(R.string.dynamic_color),
                        summary = stringResource(R.string.dynamic_color_summary),
                        icon = Icons.Rounded.Colorize,
                        checked = settings.dynamicColor,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(dynamicColor = it)) },
                    )
                }
                row { index, count ->
                    var languageMenu by remember { mutableStateOf(false) }
                    ListGroupNavRow(
                        index = index,
                        count = count,
                        title = stringResource(R.string.language),
                        value = languageLabel(settings.language),
                        icon = Icons.Rounded.Language,
                        onClick = { languageMenu = true },
                    )
                    if (languageMenu) {
                        LanguageDialog(
                            selected = settings.language,
                            onDismiss = { languageMenu = false },
                            onSelect = { language ->
                                languageMenu = false
                                viewModel.updateSettings(settings.copy(language = language))
                                // The locale is applied in attachBaseContext, so the
                                // activity has to be rebuilt for it to take effect.
                                context.findActivity()?.recreate()
                            },
                        )
                    }
                }
            }

            ListGroup(title = stringResource(R.string.sync)) {
                row { index, count ->
                    OptionGroupRow(
                        index = index,
                        count = count,
                        title = stringResource(R.string.fetch_mode),
                        icon = Icons.Rounded.Speed,
                        options = SyncFetchMode.entries,
                        selected = settings.syncFetchMode,
                        onSelect = { viewModel.updateSettings(settings.copy(syncFetchMode = it)) },
                        label = { syncModeLabel(it) },
                    )
                }
                row { index, count ->
                    ListGroupSwitchRow(
                        index = index,
                        count = count,
                        title = stringResource(R.string.save_avatar_history),
                        summary = stringResource(R.string.save_avatar_history_summary),
                        icon = Icons.Rounded.Photo,
                        checked = settings.saveAvatarHistory,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(saveAvatarHistory = it)) },
                    )
                }
                row { index, count ->
                    ListGroupSwitchRow(
                        index = index,
                        count = count,
                        title = stringResource(R.string.save_banner_history),
                        summary = stringResource(R.string.save_banner_history_summary),
                        icon = Icons.Rounded.Image,
                        checked = settings.saveBannerHistory,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(saveBannerHistory = it)) },
                    )
                }
                row { index, count ->
                    ListGroupSwitchRow(
                        index = index,
                        count = count,
                        title = stringResource(R.string.use_high_quality_avatars),
                        summary = stringResource(R.string.use_high_quality_avatars_summary),
                        icon = Icons.Rounded.HighQuality,
                        checked = settings.avatarQualityHigh,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(avatarQualityHigh = it)) },
                    )
                }
                row { index, count ->
                    OptionGroupRow(
                        index = index,
                        count = count,
                        title = stringResource(R.string.media_scope),
                        icon = Icons.Rounded.PermMedia,
                        options = MediaSaveScope.entries,
                        selected = settings.mediaSaveScope,
                        onSelect = { viewModel.updateSettings(settings.copy(mediaSaveScope = it)) },
                        label = { mediaScopeLabel(it) },
                    )
                }
            }

            ListGroup(title = stringResource(R.string.http_requests)) {
                row { index, count ->
                    ListGroupSwitchRow(
                        index = index,
                        count = count,
                        title = stringResource(R.string.use_curl_cffi_proxy),
                        summary = stringResource(R.string.use_curl_cffi_proxy_summary),
                        icon = Icons.Rounded.Bolt,
                        checked = settings.apiRequestMode == CURL_CFFI,
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings(
                                settings.copy(apiRequestMode = if (enabled) CURL_CFFI else DIO),
                            )
                        },
                    )
                }
                row { index, count ->
                    ListGroupNavRow(
                        index = index,
                        count = count,
                        title = stringResource(R.string.refresh_query_ids),
                        summary = stringResource(R.string.refresh_query_ids_summary),
                        icon = Icons.Rounded.Sync,
                        enabled = !state.isBusy,
                        onClick = viewModel::refreshQueryIds,
                    )
                }
            }

            // Only meaningful while the proxy is on, so it slides in with it.
            AnimatedVisibility(visible = settings.apiRequestMode == CURL_CFFI) {
                ListGroup {
                    row { index, count ->
                        ListGroupItem(index, count) {
                            var url by remember(settings.remoteFastApiUrl) {
                                mutableStateOf(settings.remoteFastApiUrl)
                            }
                            RoundTextField(
                                value = url,
                                onValueChange = {
                                    url = it
                                    viewModel.updateSettings(settings.copy(remoteFastApiUrl = it))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = stringResource(R.string.fastapi_url),
                            )
                        }
                    }
                    row { index, count ->
                        ListGroupItem(index, count) {
                            var key by remember(settings.fastApiApiKey) {
                                mutableStateOf(settings.fastApiApiKey)
                            }
                            RoundTextField(
                                value = key,
                                onValueChange = {
                                    key = it
                                    viewModel.updateSettings(settings.copy(fastApiApiKey = it))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = stringResource(R.string.fastapi_api_key),
                                visualTransformation = PasswordVisualTransformation(),
                            )
                        }
                    }
                }
            }

            ListGroup(title = stringResource(R.string.data_management)) {
                row { index, count ->
                    ListGroupNavRow(
                        index = index,
                        count = count,
                        title = stringResource(R.string.backup),
                        summary = stringResource(R.string.backup_summary),
                        icon = Icons.Rounded.Backup,
                        enabled = !state.isBusy,
                        onClick = { backupLauncher.launch("andronitor_backup.zip") },
                    )
                }
                row { index, count ->
                    ListGroupNavRow(
                        index = index,
                        count = count,
                        title = stringResource(R.string.restore),
                        summary = stringResource(R.string.restore_summary),
                        icon = Icons.Rounded.Restore,
                        enabled = !state.isBusy,
                        onClick = { restoreLauncher.launch(arrayOf("application/zip")) },
                    )
                }
            }

            ListGroup(title = stringResource(R.string.about)) {
                row { index, count ->
                    ListGroupRow(
                        index = index,
                        count = count,
                        title = stringResource(R.string.app_name),
                        summary = stringResource(R.string.version) + " " + BuildConfig.VERSION_NAME,
                        icon = Icons.Rounded.Info,
                    )
                }
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    modifier = Modifier.padding(horizontal = ListGroupPadding + 14.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LanguageDialog(selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val languages = listOf(
        AppLocale.LANGUAGE_SYSTEM,
        AppLocale.LANGUAGE_SIMPLIFIED_CHINESE,
        AppLocale.LANGUAGE_ENGLISH,
    )
    AndroAlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Language, null) },
        title = { Text(stringResource(R.string.language)) },
        text = {
            ListGroup(horizontalPadding = 0.dp) {
                languages.forEach { language ->
                    row { index, count ->
                        ListGroupRow(
                            index = index,
                            count = count,
                            title = languageLabel(language),
                            containerColor = if (language == selected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            onClick = { onSelect(language) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

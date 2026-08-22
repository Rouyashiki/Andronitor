package com.anatdx.andronitor.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.HeartBroken
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NoAccounts
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.anatdx.andronitor.AppLocale
import com.anatdx.andronitor.R
import com.anatdx.andronitor.model.ChangeType
import com.anatdx.andronitor.model.MediaSaveScope
import com.anatdx.andronitor.model.SyncFetchMode
import com.anatdx.andronitor.model.ThemeMode
import java.text.DateFormat
import java.util.Date

// Localized labels shared by every screen. Kept together so a new enum value
// cannot pick up an English fallback in one screen and a translation in another.

@Composable
internal fun tabLabel(tab: AppTab): String = stringResource(
    when (tab) {
        AppTab.HOME -> R.string.nav_home
        AppTab.SEARCH -> R.string.nav_search
        AppTab.ACCOUNTS -> R.string.nav_accounts
        AppTab.SETTINGS -> R.string.nav_settings
    },
)

@Composable
internal fun categoryTitle(key: String?): String = when (key) {
    "followers" -> stringResource(R.string.followers)
    "following" -> stringResource(R.string.following)
    else -> changeTypeTitle(ChangeType.fromKey(key.orEmpty()))
}

@Composable
internal fun changeTypeTitle(type: ChangeType): String = stringResource(
    when (type) {
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
    },
)

/** One icon per change type, so a long list of categories stays scannable. */
internal fun changeTypeIcon(type: ChangeType): ImageVector = when (type) {
    ChangeType.PROFILE_UPDATE -> Icons.Rounded.ManageAccounts
    ChangeType.NORMAL_UNFOLLOWED -> Icons.Rounded.PersonRemove
    ChangeType.MUTUAL_UNFOLLOWED -> Icons.Rounded.HeartBroken
    ChangeType.ONEWAY_UNFOLLOWED -> Icons.Rounded.LinkOff
    ChangeType.TEMPORARILY_RESTRICTED -> Icons.Rounded.WarningAmber
    ChangeType.SUSPENDED -> Icons.Rounded.Block
    ChangeType.DEACTIVATED -> Icons.Rounded.NoAccounts
    ChangeType.BE_FOLLOWED_BACK -> Icons.Rounded.PersonAdd
    ChangeType.NEW_FOLLOWERS_FOLLOWING -> Icons.Rounded.GroupAdd
    ChangeType.RECOVERED -> Icons.Rounded.Restore
    ChangeType.OTHER_REASONS -> Icons.Rounded.MoreHoriz
}

@Composable
internal fun syncModeLabel(mode: SyncFetchMode): String = stringResource(
    when (mode) {
        SyncFetchMode.FAST -> R.string.mode_fast
        SyncFetchMode.EXPERT -> R.string.mode_expert
        SyncFetchMode.FASTEST -> R.string.mode_fastest
    },
)

@Composable
internal fun languageLabel(language: String): String = stringResource(
    when (language) {
        AppLocale.LANGUAGE_ENGLISH -> R.string.language_english
        AppLocale.LANGUAGE_SIMPLIFIED_CHINESE -> R.string.language_simplified_chinese
        else -> R.string.language_system
    },
)

@Composable
internal fun mediaScopeLabel(scope: MediaSaveScope): String = stringResource(
    when (scope) {
        MediaSaveScope.ALL -> R.string.scope_all
        MediaSaveScope.FOLLOWING_ONLY -> R.string.scope_following_only
        MediaSaveScope.MUTUAL_ONLY -> R.string.scope_mutual_only
    },
)

@Composable
internal fun themeModeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    },
)

/**
 * The sync pipeline reports progress as English stage names, so they get mapped
 * back to resources here rather than plumbing resource ids through the worker.
 */
@Composable
internal fun localizedProgressText(text: String): String {
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
    return if (userCount != null && text.endsWith(" users")) {
        stringResource(R.string.progress_users_count, userCount)
    } else {
        text
    }
}

internal fun formatDateTime(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

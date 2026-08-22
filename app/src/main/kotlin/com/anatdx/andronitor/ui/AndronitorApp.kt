@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.anatdx.andronitor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anatdx.andronitor.model.ChangeReport
import com.anatdx.andronitor.model.TwitterUser
import com.anatdx.andronitor.ui.theme.AndronitorTheme

@Composable
fun AndronitorApp(viewModel: AndronitorViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    // Screens are derived from state rather than a back stack, so the holder is
    // what keeps each one's scroll position alive across a push and pop.
    val stateHolder = rememberSaveableStateHolder()

    AndronitorTheme(
        themeMode = state.settings.themeMode,
        dynamicColor = state.settings.dynamicColor,
    ) {
        // Every screen's payload is captured in the Screen instance so the one
        // sliding out can still draw itself after the state that produced it is
        // gone (closing a profile nulls selectedUser immediately).
        val screen = currentScreen(state)

        when (screen) {
            is Screen.Main -> Unit
            is Screen.Category -> BackHandler { viewModel.clearCategory() }
            is Screen.Profile -> BackHandler { viewModel.closeUser() }
            is Screen.History -> BackHandler { viewModel.closeUserHistory() }
            is Screen.Snapshot -> BackHandler { viewModel.closeSnapshot() }
        }

        AnimatedContent(
            targetState = screen,
            contentKey = { it.key },
            transitionSpec = {
                // Deeper screens come in from the trailing edge; going back
                // reverses it, so the gesture direction always matches the depth.
                val forward = targetState.depth >= initialState.depth
                val enterOffset: (Int) -> Int = { if (forward) it / 3 else -it / 4 }
                val exitOffset: (Int) -> Int = { if (forward) -it / 4 else it / 3 }
                (slideInHorizontally(initialOffsetX = enterOffset) + fadeIn())
                    .togetherWith(slideOutHorizontally(targetOffsetX = exitOffset) + fadeOut())
            },
            label = "screen",
        ) { target ->
            stateHolder.Render(target, state, viewModel)
        }
    }
}

@Composable
private fun SaveableStateHolder.Render(
    screen: Screen,
    state: AndronitorUiState,
    viewModel: AndronitorViewModel,
) = SaveableStateProvider(screen.key) {
    when (screen) {
        is Screen.Main -> MainScaffold(state, viewModel)
        is Screen.Category -> CategoryScreen(
            title = screen.title,
            users = screen.users,
            loading = screen.loading,
            onBack = viewModel::clearCategory,
            onUserClick = viewModel::showUser,
        )
        is Screen.Profile -> ProfileScreen(
            user = screen.user,
            ownerId = screen.ownerId,
            onBack = viewModel::closeUser,
            onShowHistory = viewModel::showUserHistory,
        )
        is Screen.History -> UserHistoryScreen(
            user = screen.user,
            reports = screen.reports,
            loading = screen.loading,
            onBack = viewModel::closeUserHistory,
            onOpenSnapshot = viewModel::showSnapshot,
        )
        is Screen.Snapshot -> ProfileScreen(
            user = screen.user,
            ownerId = screen.ownerId,
            onBack = viewModel::closeSnapshot,
            snapshotReport = screen.report,
        )
    }
}

@Composable
private fun MainScaffold(state: AndronitorUiState, viewModel: AndronitorViewModel) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Each tab screen has its own Scaffold whose top bar consumes the status
        // bar inset. Letting this outer Scaffold consume it too would pad the top
        // twice and leave a dead strip above every header.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        bottomBar = {
            ShortNavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                windowInsets = WindowInsets.navigationBars.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ),
                arrangement = ShortNavigationBarArrangement.EqualWeight,
            ) {
                Tabs.forEach { (tab, icon) ->
                    val label = tabLabel(tab)
                    ShortNavigationBarItem(
                        selected = state.tab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = state.tab,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val enterOffset: (Int) -> Int = { if (forward) it / 6 else -it / 6 }
                val exitOffset: (Int) -> Int = { if (forward) -it / 6 else it / 6 }
                (slideInHorizontally(initialOffsetX = enterOffset) + fadeIn())
                    .togetherWith(slideOutHorizontally(targetOffsetX = exitOffset) + fadeOut())
            },
            label = "tab",
        ) { tab ->
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    AppTab.HOME -> HomeScreen(state, viewModel)
                    AppTab.SEARCH -> SearchScreen(state, viewModel)
                    AppTab.ACCOUNTS -> AccountsScreen(state, viewModel)
                    AppTab.SETTINGS -> SettingsScreen(state, viewModel)
                }
            }
        }
    }
}

private val Tabs: List<Pair<AppTab, ImageVector>> = listOf(
    AppTab.HOME to Icons.Rounded.Home,
    AppTab.SEARCH to Icons.Rounded.Search,
    AppTab.ACCOUNTS to Icons.Rounded.Group,
    AppTab.SETTINGS to Icons.Rounded.Settings,
)

/**
 * A screen plus everything it needs to draw. [depth] gives the router a push/pop
 * direction and [key] identifies it for both the transition and the state holder.
 */
private sealed class Screen(val key: String, val depth: Int) {
    data object Main : Screen("main", 0)

    class Category(
        val title: String,
        val users: List<TwitterUser>,
        val loading: Boolean,
    ) : Screen("category:$title", 1)

    class Profile(val user: TwitterUser, val ownerId: String?) :
        Screen("profile:${user.restId}", 2)

    class History(
        val user: TwitterUser,
        val reports: List<ChangeReport>,
        val loading: Boolean,
    ) : Screen("history:${user.restId}", 3)

    class Snapshot(val user: TwitterUser, val report: ChangeReport, val ownerId: String?) :
        Screen("snapshot:${report.id}", 4)
}

@Composable
private fun currentScreen(state: AndronitorUiState): Screen {
    val snapshot = state.snapshot
    val historyUser = state.historyUser
    val selectedUser = state.selectedUser
    val category = state.selectedCategory
    // categoryTitle reads resources, so it has to be resolved out here rather
    // than inside the Screen constructor.
    val title = if (category != null) categoryTitle(category) else ""
    return when {
        snapshot != null -> Screen.Snapshot(snapshot.user, snapshot.report, state.activeAccount?.id)
        historyUser != null -> Screen.History(historyUser, state.historyReports, state.historyLoading)
        selectedUser != null -> Screen.Profile(selectedUser, state.activeAccount?.id)
        category != null -> Screen.Category(title, state.users, state.isLoading)
        else -> Screen.Main
    }
}

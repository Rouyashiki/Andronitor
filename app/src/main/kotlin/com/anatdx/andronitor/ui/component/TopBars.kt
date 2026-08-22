@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.animation.ExperimentalAnimationApi::class,
)

package com.anatdx.andronitor.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.anatdx.andronitor.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Scroll behavior for a [CollapsingTopBar]: the large header shrinks away
 * entirely as the content scrolls up, then comes back on the way down.
 */
@Composable
fun rememberCollapsingScrollBehavior(): TopAppBarScrollBehavior =
    TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

@Composable
private fun flatTopAppBarColors(): TopAppBarColors {
    val background = MaterialTheme.colorScheme.background
    // Same colour scrolled or not: the list groups below already carry the
    // elevation, so tinting the bar as well just muddies the header.
    return TopAppBarDefaults.topAppBarColors(
        containerColor = background,
        scrolledContainerColor = background,
    )
}

private val TopBarInsets
    @Composable get() = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
    )

/**
 * The app's standard header: a Material 3 Expressive large flexible top app bar
 * that collapses into a compact one as its content scrolls.
 *
 * Deliberately title-only. The flexible app bar reserves a taller expanded height
 * as soon as a subtitle is present, and a second line of header text has so far
 * only ever repeated something already on screen.
 *
 * The host must feed it scroll deltas — put
 * `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` on the Scaffold.
 */
@Composable
fun CollapsingTopBar(
    title: String,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    LargeFlexibleTopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        navigationIcon = { BackButton(onBack) },
        actions = actions,
        windowInsets = TopBarInsets,
        colors = flatTopAppBarColors(),
        scrollBehavior = scrollBehavior,
    )
}

/**
 * Plain 64 dp bar, matching what YukiSU puts on its home screen. The large
 * flexible header costs 120 dp before insets, which is a lot of chrome for a
 * screen whose first card already names the account.
 */
@Composable
fun CompactTopBar(
    title: String,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        navigationIcon = { BackButton(onBack) },
        actions = actions,
        windowInsets = TopBarInsets,
        colors = flatTopAppBarColors(),
        scrollBehavior = scrollBehavior,
    )
}

/** Pinned behavior for [CompactTopBar]: it recolours on scroll but never resizes. */
@Composable
fun rememberPinnedScrollBehavior(): TopAppBarScrollBehavior =
    TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

@Composable
private fun BackButton(onBack: (() -> Unit)?) {
    if (onBack == null) return
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.browser_back))
    }
}

/**
 * Header that morphs between a large title and a capsule search field.
 *
 * The two halves are animated separately on purpose: collapsing the tall header
 * and growing the field at the same time makes the field start off-screen, so
 * the header settles first and only then does the field expand from the trailing
 * edge. On the way out the field shrinks on a fixed 180 ms curve and the header
 * starts expanding slightly before that finishes, which hides the hand-off.
 */
@Composable
fun SearchTopBar(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var searching by remember { mutableStateOf(false) }
    var compactHeader by remember { mutableStateOf(false) }
    var fieldVisible by remember { mutableStateOf(false) }

    val headerTransition = updateTransition(compactHeader, label = "SearchHeader")
    val fieldTransition = updateTransition(fieldVisible, label = "SearchField")

    LaunchedEffect(searching) {
        if (searching) {
            compactHeader = true
            snapshotFlow { headerTransition.currentState && !headerTransition.isRunning }.first { it }
            fieldVisible = true
        } else {
            fieldVisible = false
            delay(150)
            compactHeader = false
        }
    }
    LaunchedEffect(fieldVisible) {
        if (fieldVisible) {
            delay(50)
            focusRequester.requestFocus()
        }
    }
    DisposableEffect(Unit) { onDispose { keyboard?.hide() } }

    val searchField: @Composable () -> Unit = {
        RoundTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp, horizontal = 8.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) searching = true },
            placeholder = placeholder,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboard?.hide()
                onSubmit()
            }),
            trailingIcon = {
                IconButton(
                    onClick = {
                        searching = false
                        keyboard?.hide()
                        onClear()
                    },
                ) { Icon(Icons.Rounded.Close, stringResource(R.string.close)) }
            },
        )
    }

    headerTransition.AnimatedContent(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
        transitionSpec = {
            val effects = tween<Float>(durationMillis = 90)
            fadeIn(animationSpec = effects)
                .togetherWith(fadeOut(animationSpec = effects))
                .using(
                    SizeTransform(clip = false) { _, _ ->
                        tween(durationMillis = 110, easing = FastOutSlowInEasing)
                    },
                )
        },
    ) { compact ->
        if (compact) {
            // The compact bar must not inherit the collapsed header offset, or the
            // field can be laid out above the top of the screen.
            TopAppBar(
                title = {
                    Box {
                        AnimatedVisibility(
                            modifier = Modifier.align(Alignment.CenterStart),
                            visible = !fieldTransition.currentState && !fieldTransition.targetState,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        fieldTransition.AnimatedVisibility(
                            visible = { it },
                            enter = expandHorizontally(
                                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>(),
                                expandFrom = Alignment.End,
                                clip = true,
                                initialWidth = { fullWidth -> fullWidth / 10 },
                            ),
                            exit = shrinkHorizontally(
                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                                shrinkTowards = Alignment.End,
                                clip = true,
                                targetWidth = { 0 },
                            ),
                            content = { searchField() },
                        )
                    }
                },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    AnimatedVisibility(visible = !fieldVisible) {
                        IconButton(onClick = { searching = true }) {
                            Icon(Icons.Rounded.Search, stringResource(R.string.search))
                        }
                    }
                    actions()
                },
                windowInsets = TopBarInsets,
                colors = flatTopAppBarColors(),
            )
        } else {
            LargeFlexibleTopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = { searching = true }) {
                        Icon(Icons.Rounded.Search, stringResource(R.string.search))
                    }
                    actions()
                },
                windowInsets = TopBarInsets,
                colors = flatTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        }
    }
}

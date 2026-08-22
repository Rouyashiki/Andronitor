@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.anatdx.andronitor.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The app's progress vocabulary, all Material 3 Expressive: wavy bars and rings
 * for real progress, the morphing loading indicator for indeterminate waits.
 *
 * Passing a null [progress] switches to the indeterminate variant — the sync
 * pipeline reports stages that have no total to divide by.
 */
@Composable
fun WavyLinearProgress(modifier: Modifier = Modifier, progress: (() -> Float)? = null) {
    if (progress == null) {
        LinearWavyProgressIndicator(modifier = modifier.fillMaxWidth())
    } else {
        LinearWavyProgressIndicator(progress = progress, modifier = modifier.fillMaxWidth())
    }
}

@Composable
fun WavyCircularProgress(modifier: Modifier = Modifier, progress: (() -> Float)? = null) {
    if (progress == null) {
        CircularWavyProgressIndicator(modifier = modifier)
    } else {
        CircularWavyProgressIndicator(progress = progress, modifier = modifier)
    }
}

/** Centered indeterminate indicator for a screen that has nothing to show yet. */
@Composable
fun AndroLoadingIndicator(modifier: Modifier = Modifier) {
    ContainedLoadingIndicator(modifier = modifier)
}

@Composable
fun AndroPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
        content = content,
    )
}

/** Fills its parent and centers an [AndroLoadingIndicator]. */
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) { AndroLoadingIndicator() }
}

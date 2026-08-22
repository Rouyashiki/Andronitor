@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.anatdx.andronitor.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anatdx.andronitor.R

/**
 * Material 3 Expressive button group used wherever the app picks one value out
 * of a short list — fetch mode, media scope, theme mode, tri-state filters.
 *
 * The group animates the pressed button wider and spills into an overflow menu
 * when the labels do not fit, which is why this is a [ButtonGroup] rather than a
 * row of chips.
 */
@Composable
fun <T> OptionGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (T) -> String,
) {
    // ButtonGroup's content block is a plain builder, not a composable scope, so
    // the labels have to be resolved before we get there.
    val labels = options.map { label(it) }
    // Deliberately not fillMaxWidth: the group reserves room for the overflow
    // indicator by shrinking maxWidth, which throws if minWidth was pinned to the
    // same value. It sizes to its buttons and overflows on its own when needed.
    ButtonGroup(
        overflowIndicator = { menuState ->
            IconButton(onClick = { if (menuState.isShowing) menuState.dismiss() else menuState.show() }) {
                Icon(Icons.Rounded.MoreVert, stringResource(R.string.filter))
            }
        },
        modifier = modifier,
    ) {
        options.forEachIndexed { index, option ->
            toggleableItem(
                option == selected,
                labels[index],
                { checked -> if (checked) onSelect(option) },
            )
        }
    }
}

/**
 * An [OptionGroup] as one block of a list group: leading icon and title on the
 * first line, the buttons underneath.
 *
 * The block sits on `surfaceContainerLow` rather than the usual
 * `surfaceContainer` because that is exactly the colour an unselected toggle
 * button fills with — on the default row colour they would be invisible.
 */
@Composable
fun <T> OptionGroupRow(
    index: Int,
    count: Int,
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    label: @Composable (T) -> String,
) {
    ListGroupItem(
        index = index,
        count = count,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(16.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            OptionGroup(
                options = options,
                selected = selected,
                onSelect = onSelect,
                label = label,
            )
        }
    }
}

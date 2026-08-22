@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.anatdx.andronitor.ui.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anatdx.andronitor.ui.theme.ListGroupMinHeight

/** Inset that keeps a group's rounded blocks clear of the screen edge. */
val ListGroupPadding = 16.dp

/**
 * Collects the rows of a [ListGroup] so each one learns its own position, which
 * is what [ListItemDefaults.segmentedShapes] needs to round the group's outer
 * corners more than its inner ones.
 */
class ListGroupScope internal constructor() {
    internal val rows = mutableListOf<@Composable (index: Int, count: Int) -> Unit>()

    fun row(content: @Composable (index: Int, count: Int) -> Unit) {
        rows += content
    }
}

/**
 * A Material 3 Expressive list group: an optional coloured section label above a
 * stack of separately rounded blocks, replacing the older "one card, dividers
 * inside" pattern.
 */
@Composable
fun ListGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    horizontalPadding: Dp = ListGroupPadding,
    content: ListGroupScope.() -> Unit,
) {
    val scope = ListGroupScope().apply(content)
    val count = scope.rows.size
    if (count == 0) return
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 10.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        scope.rows.forEachIndexed { index, row -> key(index) { row(index, count) } }
    }
}

/**
 * One block of a [ListGroup]. Rounds its corners from its position in the group
 * and leaves a [ListItemDefaults.SegmentedGap] sliver of background above and
 * below so the blocks read as separate.
 */
@Composable
fun ListGroupItem(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    enabled: Boolean = true,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit,
) {
    // A lone row has no neighbours to point at, so it gets uniform corners.
    val shape = if (count <= 1) {
        MaterialTheme.shapes.large
    } else {
        ListItemDefaults.segmentedShapes(index, count).shape
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ListItemDefaults.SegmentedGap / 2)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(containerColor)
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        enabled = enabled,
                        role = role,
                        onClick = onClick,
                    )
                },
            )
            .defaultMinSize(minHeight = ListGroupMinHeight)
            .padding(contentPadding),
        verticalAlignment = verticalAlignment,
        content = content,
    )
}

/**
 * The standard row: optional leading icon, title over summary, optional trailing
 * slot. Everything else in this file is a thin wrapper over it.
 */
@Composable
fun ListGroupRow(
    index: Int,
    count: Int,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    titleColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    ListGroupItem(
        index = index,
        count = count,
        modifier = modifier,
        containerColor = containerColor,
        enabled = enabled,
        role = role,
        interactionSource = interactionSource,
        onClick = onClick,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = iconTint)
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/** A row that navigates somewhere, marked by the trailing chevron. */
@Composable
fun ListGroupNavRow(
    index: Int,
    count: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    value: String? = null,
    enabled: Boolean = true,
) {
    ListGroupRow(
        index = index,
        count = count,
        title = title,
        modifier = modifier,
        summary = summary,
        icon = icon,
        enabled = enabled,
        onClick = onClick,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Icon(
                    Icons.AutoMirrored.Rounded.NavigateNext,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

/** A row whose whole surface toggles an [AndroSwitch]. */
@Composable
fun ListGroupSwitchRow(
    index: Int,
    count: Int,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    // Shared so pressing anywhere on the row ripples the switch too.
    val interactionSource = remember { MutableInteractionSource() }
    ListGroupRow(
        index = index,
        count = count,
        title = title,
        modifier = modifier,
        summary = summary,
        icon = icon,
        iconTint = if (checked) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        enabled = enabled,
        role = Role.Switch,
        interactionSource = interactionSource,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            AndroSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                interactionSource = interactionSource,
            )
        },
    )
}

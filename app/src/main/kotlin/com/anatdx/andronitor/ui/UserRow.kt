package com.anatdx.andronitor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anatdx.andronitor.R
import com.anatdx.andronitor.model.TwitterUser
import com.anatdx.andronitor.ui.component.ListGroupItem

/** A tracked user rendered as one block of a segmented list group. */
@Composable
internal fun UserGroupRow(
    index: Int,
    count: Int,
    user: TwitterUser,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ListGroupItem(index = index, count = count, modifier = modifier, onClick = onClick) {
        RemoteAvatar(user.avatarUrl, user.avatarLocalPath, 48.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = user.name ?: stringResource(R.string.unknown),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${user.screenName ?: user.restId}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            user.bio?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (user.isFollower || user.isFollowing) {
            Spacer(Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (user.isFollower) RelationshipPill(stringResource(R.string.follower), filled = true)
                if (user.isFollowing) RelationshipPill(stringResource(R.string.following), filled = false)
            }
        }
    }
}

/** Small capsule marking a follower/following relationship. */
@Composable
internal fun RelationshipPill(text: String, filled: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = if (filled) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        contentColor = if (filled) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = if (filled) {
            null
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/** Row of the two relationship pills, laid out inline for profile headers. */
@Composable
internal fun RelationshipPills(user: TwitterUser, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (user.isFollower) RelationshipPill(stringResource(R.string.follows_you), filled = true)
        if (user.isFollowing) RelationshipPill(stringResource(R.string.following), filled = false)
    }
}

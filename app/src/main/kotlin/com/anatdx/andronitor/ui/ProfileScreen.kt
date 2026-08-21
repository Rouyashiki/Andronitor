@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.anatdx.andronitor.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.NoAccounts
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.anatdx.andronitor.R
import com.anatdx.andronitor.model.ChangeReport
import com.anatdx.andronitor.model.ChangeType
import com.anatdx.andronitor.model.TwitterUser
import org.json.JSONObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val VerifiedBlue = Color(0xFF1DA1F2)

@Composable
fun ProfileScreen(
    user: TwitterUser,
    ownerId: String?,
    onBack: () -> Unit,
    onShowHistory: ((TwitterUser) -> Unit)? = null,
    snapshotReport: ChangeReport? = null,
) {
    var showJson by remember { mutableStateOf(false) }
    var showVisit by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var viewerModel by remember { mutableStateOf<Any?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user.name ?: stringResource(R.string.profile)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.browser_back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, null) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("JSON") },
                                leadingIcon = { Icon(Icons.Outlined.Code, null) },
                                onClick = {
                                    menuOpen = false
                                    showJson = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            ProfileHeader(
                user = user,
                showHistoryButton = onShowHistory != null && snapshotReport == null,
                onShowHistory = { onShowHistory?.invoke(user) },
                onVisit = { showVisit = true },
                onViewImage = { viewerModel = it },
            )
            Spacer(Modifier.height(5.dp))
            UserInfoColumn(user)
            StatusPrompts(user)
            if (user.status !in setOf("suspended", "deactivated", "temporarily_restricted") &&
                user.keptIdsStatus != "temporarily_restricted"
            ) {
                Spacer(Modifier.height(12.dp))
            }
            MetadataRow(user, ownerId)
            Spacer(Modifier.height(5.dp))
            StatGrid(user)
            user.pinnedTweetId?.let { pinned ->
                SectionLabel(stringResource(R.string.user_content))
                InfoTile(Icons.Filled.PushPin, stringResource(R.string.pinned_tweet_id), pinned)
            }
            SectionLabel(stringResource(R.string.identity))
            InfoTile(Icons.Filled.Fingerprint, "Rest ID", user.restId)
            snapshotReport?.let { report ->
                Text(
                    "${changeTypeTitle(ChangeType.fromKey(report.changeType))}\n${formatTimestamp(report.timestamp)}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showJson) ProfileJsonSheet(user) { showJson = false }
    if (showVisit) VisitDialog(user) { showVisit = false }
    viewerModel?.let { model -> FullScreenImageViewer(model) { viewerModel = null } }
}

@Composable
private fun ProfileHeader(
    user: TwitterUser,
    showHistoryButton: Boolean,
    onShowHistory: () -> Unit,
    onVisit: () -> Unit,
    onViewImage: (Any) -> Unit,
) {
    val context = LocalContext.current
    val highQualityAvatarUrl = user.avatarUrl?.replace(Regex("_(normal|bigger|400x400)"), "_400x400")
    val bannerModel = resolveMediaModel(context, user.bannerUrl, user.bannerLocalPath)
    val avatarModel = resolveMediaModel(context, highQualityAvatarUrl, user.avatarLocalPath)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val bannerHeight = maxWidth / 3
        Column(Modifier.fillMaxWidth()) {
            RemoteBanner(
                url = user.bannerUrl,
                localPath = user.bannerLocalPath,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeight)
                    .clickable(enabled = bannerModel != null) { bannerModel?.let(onViewImage) },
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, end = 16.dp)
                    .heightIn(min = 45.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top,
            ) {
                if (showHistoryButton) {
                    FilledTonalButton(
                        onClick = onShowHistory,
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(Icons.Outlined.History, stringResource(R.string.history), Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }
                FilledTonalButton(
                    onClick = onVisit,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.visit))
                }
            }
        }
        Surface(
            modifier = Modifier
                .padding(start = 16.dp)
                .offset(y = bannerHeight - 50.dp)
                .size(90.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Box(Modifier.padding(3.dp)) {
                RemoteAvatar(
                    url = highQualityAvatarUrl,
                    localPath = user.avatarLocalPath,
                    size = 84.dp,
                    modifier = Modifier.clickable(enabled = avatarModel != null) { avatarModel?.let(onViewImage) },
                )
            }
        }
    }
}

@Composable
private fun UserInfoColumn(user: TwitterUser) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        NameHeader(user)
        ScreenNameRow(user)
        user.automatedScreenName?.takeIf { it.isNotBlank() }?.let { AutomationLabel(it) }
        user.parodyLabel?.takeIf { it.isNotBlank() && it != "None" }?.let { ParodyLabel(it) }
        user.bio?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            BioRichText(it, user.bioLinks)
        }
    }
}

@Composable
private fun NameHeader(user: TwitterUser) {
    val text = buildAnnotatedString {
        append(user.name ?: stringResource(R.string.unknown))
        if (user.isVerified) {
            append(" ")
            appendInlineContent("verified", "✔")
        }
        if (user.isProtected) {
            append(" ")
            appendInlineContent("protected", "🔒")
        }
    }
    val inlineContent = mapOf(
        "verified" to InlineTextContent(
            Placeholder(24.sp, 24.sp, PlaceholderVerticalAlign.TextCenter),
        ) { Icon(Icons.Filled.Verified, stringResource(R.string.verified), tint = VerifiedBlue) },
        "protected" to InlineTextContent(
            Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.TextCenter),
        ) { Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.onSurface) },
    )
    SelectionContainer {
        Text(
            text,
            inlineContent = inlineContent,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ScreenNameRow(user: TwitterUser) {
    FlowRow(verticalArrangement = Arrangement.Center) {
        SelectionContainer(Modifier.align(Alignment.CenterVertically)) {
            Text(
                "@${user.screenName ?: user.restId}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (user.isFollower) {
            RelationshipChip(
                stringResource(R.string.follows_you),
                outlined = false,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 4.dp),
            )
        }
        if (user.isFollowing) {
            RelationshipChip(
                stringResource(R.string.following),
                outlined = true,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun RelationshipChip(text: String, outlined: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = if (outlined) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHighest,
        border = if (outlined) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (outlined) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AutomationLabel(automatedScreenName: String) {
    val template = stringResource(R.string.automated_by)
    val parts = template.split("%1\$s")
    val primary = MaterialTheme.colorScheme.primary
    val text = buildAnnotatedString {
        append(parts.first())
        withLink(
            LinkAnnotation.Url(
                "https://x.com/$automatedScreenName",
                TextLinkStyles(SpanStyle(color = primary)),
            ),
        ) { append("@$automatedScreenName") }
        if (parts.size > 1) append(parts.last())
    }
    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.SmartToy, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ParodyLabel(label: String) {
    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.TheaterComedy, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.parody_account_format, label), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BioRichText(bio: String, bioLinks: List<String>) {
    val primary = MaterialTheme.colorScheme.primary
    val annotated = remember(bio, bioLinks, primary) { linkifyBio(bio, bioLinks, primary) }
    SelectionContainer {
        Text(annotated, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Colors and links URLs and @mentions inside a bio, mirroring the upstream rich text. */
internal fun linkifyBio(bio: String, bioLinks: List<String>, linkColor: Color): AnnotatedString {
    data class Entity(val range: IntRange, val url: String)

    val entities = mutableListOf<Entity>()
    fun overlaps(range: IntRange) = entities.any { range.first <= it.range.last && range.last >= it.range.first }

    // Exact entity URLs first (longest-first), like upstream — a regex can't
    // tell where a URL ends when the bio puts punctuation right after it.
    for (link in bioLinks.distinct().sortedByDescending { it.length }) {
        if (link.isEmpty()) continue
        var start = bio.indexOf(link)
        while (start != -1) {
            val range = start..(start + link.length - 1)
            if (!overlaps(range)) entities += Entity(range, link)
            start = bio.indexOf(link, start + link.length)
        }
    }
    Regex("""https?://[^\s]+""").findAll(bio).forEach { match ->
        val trimmed = match.value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '"', '\'', '。', '，', '）', '】', '、')
        if (trimmed.length > "https://".length) {
            val range = match.range.first..(match.range.first + trimmed.length - 1)
            if (!overlaps(range)) entities += Entity(range, trimmed)
        }
    }
    Regex("""@[A-Za-z0-9_]{1,15}""").findAll(bio).forEach { match ->
        if (!overlaps(match.range)) entities += Entity(match.range, "https://x.com/${match.value.drop(1)}")
    }
    entities.sortBy { it.range.first }
    return buildAnnotatedString {
        var position = 0
        for (entity in entities) {
            if (entity.range.first < position) continue
            append(bio.substring(position, entity.range.first))
            withLink(LinkAnnotation.Url(entity.url, TextLinkStyles(SpanStyle(color = linkColor)))) {
                append(bio.substring(entity.range.first, entity.range.last + 1))
            }
            position = entity.range.last + 1
        }
        if (position < bio.length) append(bio.substring(position))
    }
}

@Composable
private fun StatusPrompts(user: TwitterUser) {
    data class Prompt(val icon: ImageVector, val title: String, val subtitle: String)

    val prompts = buildList {
        if (user.status == "suspended") add(
            Prompt(
                Icons.Outlined.Lock,
                stringResource(R.string.account_suspended),
                stringResource(R.string.account_suspended_description),
            ),
        )
        if (user.status == "deactivated") add(
            Prompt(
                Icons.Outlined.NoAccounts,
                stringResource(R.string.account_deactivated),
                stringResource(R.string.account_deactivated_description),
            ),
        )
        if (user.status == "temporarily_restricted" || user.keptIdsStatus == "temporarily_restricted") add(
            Prompt(
                Icons.Outlined.WarningAmber,
                stringResource(R.string.account_temporarily_restricted),
                stringResource(R.string.account_temporarily_restricted_description),
            ),
        )
    }
    if (prompts.isEmpty()) return
    FlowRow(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        prompts.forEach { prompt ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(prompt.icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            prompt.title,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            prompt.subtitle,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.78f),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(user: TwitterUser, ownerId: String?) {
    FlowRow(
        Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        user.professionalCategory?.takeIf { it.isNotBlank() }?.let {
            MetadataItem(Icons.Outlined.BusinessCenter, it)
        }
        user.location?.takeIf { it.isNotBlank() }?.let {
            MetadataItem(Icons.Outlined.LocationOn, it)
        }
        user.link?.takeIf { it.isNotBlank() }?.let { LinkItem(it) }
        formatBirthday(user.birthdateYear, user.birthdateMonth, user.birthdateDay)?.let {
            MetadataItem(Icons.Outlined.Cake, it)
        }
        user.joinedTime?.takeIf { it.isNotBlank() }?.let {
            MetadataItem(Icons.Outlined.CalendarMonth, stringResource(R.string.joined_format, formatJoinedTime(it)))
        }
        if (ownerId != null && user.restId != ownerId) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Mail, null, Modifier.size(16.dp),
                    tint = if (user.canDm) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    Icons.Outlined.Tag, null, Modifier.size(16.dp),
                    tint = if (user.canMediaTag) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
private fun MetadataItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        SelectionContainer {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LinkItem(url: String) {
    val primary = MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Link, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        SelectionContainer {
            Text(
                buildAnnotatedString {
                    withLink(LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = primary)))) { append(url) }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun StatGrid(user: TwitterUser) {
    data class StatItem(val icon: ImageVector, val label: String, val value: Int)

    val items = listOf(
        StatItem(Icons.Outlined.Group, stringResource(R.string.following), user.followingCount),
        StatItem(Icons.Filled.Group, stringResource(R.string.followers), user.followersCount),
        StatItem(Icons.Filled.Create, stringResource(R.string.tweets), user.statusesCount),
        StatItem(Icons.Filled.Image, stringResource(R.string.media_count), user.mediaCount),
        StatItem(Icons.Filled.Favorite, stringResource(R.string.likes), user.favouritesCount),
        StatItem(Icons.AutoMirrored.Filled.ListAlt, stringResource(R.string.listed_count), user.listedCount),
    )
    Surface(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items.forEach { item ->
                Column(
                    Modifier
                        .width(100.dp)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(item.icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            item.value.toString(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 16.sp, stepSize = 0.5.sp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(item.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        Modifier.padding(start = 16.dp, top = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun InfoTile(icon: ImageVector, title: String, value: String) {
    ListItem(
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(title) },
        supportingContent = { SelectionContainer { Text(value) } },
    )
}

@Composable
private fun ProfileJsonSheet(user: TwitterUser, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val copiedMessage = stringResource(R.string.copied_to_clipboard)
    val pretty = remember(user) {
        val raw = user.toJsonString()
        runCatching { JSONObject(raw).toString(2) }.getOrDefault(raw)
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxHeight(0.85f)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            Text(
                "JSON",
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))
            Surface(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                SelectionContainer {
                    Text(
                        pretty,
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(pretty))
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                    onDismiss()
                }) {
                    Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.copy))
                }
            }
        }
    }
}

@Composable
private fun VisitDialog(user: TwitterUser, onDismiss: () -> Unit) {
    val context = LocalContext.current
    fun open(uri: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }
        onDismiss()
    }

    val screenName = user.screenName?.takeIf { it.isNotBlank() }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null) },
        title = { Text(stringResource(R.string.visit)) },
        text = {
            Column {
                val itemColors = ListItemDefaults.colors(containerColor = Color.Transparent)
                if (screenName != null) {
                    ListItem(
                        leadingContent = { Icon(Icons.Outlined.Android, null) },
                        headlineContent = { Text(stringResource(R.string.open_in_app)) },
                        supportingContent = { Text("twitter://user?screen_name=$screenName", maxLines = 2) },
                        colors = itemColors,
                        modifier = Modifier.clickable { open("twitter://user?screen_name=$screenName") },
                    )
                    ListItem(
                        leadingContent = { Icon(Icons.Outlined.AlternateEmail, null) },
                        headlineContent = { Text(stringResource(R.string.browser_via_screen_name)) },
                        supportingContent = { Text("@$screenName", maxLines = 1) },
                        colors = itemColors,
                        modifier = Modifier.clickable { open("https://x.com/$screenName") },
                    )
                }
                if (user.restId.isNotEmpty()) {
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.Fingerprint, null) },
                        headlineContent = { Text(stringResource(R.string.browser_via_rest_id)) },
                        supportingContent = { Text("ID: ${user.restId}", maxLines = 1) },
                        colors = itemColors,
                        modifier = Modifier.clickable { open("https://x.com/intent/user?user_id=${user.restId}") },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun FullScreenImageViewer(model: Any, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            SubcomposeAsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LinearProgressIndicator() } },
            )
        }
    }
}

@Composable
fun UserHistoryScreen(
    user: TwitterUser,
    reports: List<ChangeReport>,
    loading: Boolean,
    onBack: () -> Unit,
    onOpenSnapshot: (ChangeReport) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${stringResource(R.string.history)} · @${user.screenName ?: user.restId}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.browser_back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading -> LinearProgressIndicator(
                Modifier
                    .fillMaxWidth()
                    .padding(padding),
            )
            reports.isEmpty() -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.no_history_found),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(reports, key = { it.id }) { report ->
                    Card(
                        onClick = { onOpenSnapshot(report) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        ListItem(
                            leadingContent = { Icon(Icons.Outlined.History, null) },
                            headlineContent = { Text(changeTypeTitle(ChangeType.fromKey(report.changeType))) },
                            supportingContent = { Text(formatTimestamp(report.timestamp)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}

/** Formats X's "EEE MMM dd HH:mm:ss Z yyyy" creation time in the device locale. */
internal fun formatJoinedTime(raw: String): String = runCatching {
    val parsed = SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.US).parse(raw)
    if (parsed == null) raw else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(parsed)
}.getOrDefault(raw)

private fun formatTimestamp(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))

internal fun formatBirthday(year: String?, month: String?, day: String?): String? {
    val y = year?.toIntOrNull()
    val m = month?.toIntOrNull()
    val d = day?.toIntOrNull()
    return when {
        y != null && m != null && d != null -> {
            val calendar = Calendar.getInstance().apply { set(y, m - 1, d) }
            DateFormat.getDateInstance(DateFormat.SHORT).format(calendar.time)
        }
        m != null && d != null -> {
            val calendar = Calendar.getInstance().apply { set(2000, m - 1, d) }
            runCatching {
                val pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "Md")
                SimpleDateFormat(pattern, Locale.getDefault()).format(calendar.time)
            }.getOrDefault("$m/$d")
        }
        y != null -> y.toString()
        else -> null
    }
}

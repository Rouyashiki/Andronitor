@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.anatdx.andronitor.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NoAccounts
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.anatdx.andronitor.ui.component.AndroAlertDialog
import com.anatdx.andronitor.ui.component.ListGroup
import com.anatdx.andronitor.ui.component.ListGroupItem
import com.anatdx.andronitor.ui.component.ListGroupPadding
import com.anatdx.andronitor.ui.component.ListGroupRow
import com.anatdx.andronitor.ui.component.WavyCircularProgress
import org.json.JSONObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Titleless: the name and handle are the first thing under the banner
            // anyway, and leaving the bar empty is what lets it stay transparent
            // over an arbitrary user banner. It fades to a solid bar on scroll.
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(R.string.browser_back),
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, null)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("JSON") },
                                leadingIcon = { Icon(Icons.Rounded.Code, null) },
                                onClick = {
                                    menuOpen = false
                                    showJson = true
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                // The banner deliberately runs behind the app bar, so only the
                // bottom inset is consumed here.
                .padding(bottom = padding.calculateBottomPadding())
                .verticalScroll(rememberScrollState()),
        ) {
            ProfileHeader(
                user = user,
                showHistoryButton = onShowHistory != null && snapshotReport == null,
                onShowHistory = { onShowHistory?.invoke(user) },
                onVisit = { showVisit = true },
                onViewImage = { viewerModel = it },
            )
            UserInfoColumn(user)
            StatusPrompts(user)
            MetadataRow(user, ownerId)
            StatGrid(user)

            user.pinnedTweetId?.let { pinned ->
                ListGroup(title = stringResource(R.string.user_content)) {
                    row { index, count ->
                        InfoRow(index, count, Icons.Rounded.PushPin, stringResource(R.string.pinned_tweet_id), pinned)
                    }
                }
            }
            ListGroup(title = stringResource(R.string.identity)) {
                row { index, count ->
                    InfoRow(index, count, Icons.Rounded.Fingerprint, "Rest ID", user.restId)
                }
            }
            snapshotReport?.let { report ->
                Text(
                    "${changeTypeTitle(ChangeType.fromKey(report.changeType))}\n${formatDateTime(report.timestamp)}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = ListGroupPadding),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(32.dp))
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
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // The banner also covers the status bar and the transparent app bar, so
        // its drawn height has to grow by the top inset.
        val bannerHeight = maxWidth / 3 + statusBar
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
                    .padding(top = 12.dp, end = ListGroupPadding)
                    .heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top,
            ) {
                if (showHistoryButton) {
                    FilledTonalIconButton(onClick = onShowHistory) {
                        Icon(Icons.Rounded.History, stringResource(R.string.history))
                    }
                    Spacer(Modifier.width(8.dp))
                }
                FilledTonalButton(onClick = onVisit) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.visit))
                }
            }
        }
        Surface(
            modifier = Modifier
                .padding(start = ListGroupPadding)
                .offset(y = bannerHeight - 52.dp)
                .size(96.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(Modifier.padding(4.dp)) {
                RemoteAvatar(
                    url = highQualityAvatarUrl,
                    localPath = user.avatarLocalPath,
                    size = 88.dp,
                    modifier = Modifier.clickable(enabled = avatarModel != null) {
                        avatarModel?.let(onViewImage)
                    },
                )
            }
        }
    }
}

@Composable
private fun UserInfoColumn(user: TwitterUser) {
    Column(
        Modifier.padding(horizontal = ListGroupPadding, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        NameHeader(user)
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectionContainer {
                Text(
                    "@${user.screenName ?: user.restId}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            RelationshipPills(user)
        }
        user.automatedScreenName?.takeIf { it.isNotBlank() }?.let { AutomationLabel(it) }
        user.parodyLabel?.takeIf { it.isNotBlank() && it != "None" }?.let { ParodyLabel(it) }
        user.bio?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(6.dp))
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
        ) { Icon(Icons.Rounded.Verified, stringResource(R.string.verified), tint = VerifiedBlue) },
        "protected" to InlineTextContent(
            Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.TextCenter),
        ) { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.onSurface) },
    )
    SelectionContainer {
        Text(text, inlineContent = inlineContent, style = MaterialTheme.typography.headlineSmall)
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
    IconLabelRow(Icons.Rounded.SmartToy) { Text(text, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun ParodyLabel(label: String) {
    IconLabelRow(Icons.Rounded.TheaterComedy) {
        Text(
            stringResource(R.string.parody_account_format, label),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun IconLabelRow(icon: ImageVector, content: @Composable () -> Unit) {
    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(6.dp))
        content()
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
                Icons.Rounded.Lock,
                stringResource(R.string.account_suspended),
                stringResource(R.string.account_suspended_description),
            ),
        )
        if (user.status == "deactivated") add(
            Prompt(
                Icons.Rounded.NoAccounts,
                stringResource(R.string.account_deactivated),
                stringResource(R.string.account_deactivated_description),
            ),
        )
        if (user.status == "temporarily_restricted" || user.keptIdsStatus == "temporarily_restricted") add(
            Prompt(
                Icons.Rounded.WarningAmber,
                stringResource(R.string.account_temporarily_restricted),
                stringResource(R.string.account_temporarily_restricted_description),
            ),
        )
    }
    if (prompts.isEmpty()) return
    ListGroup {
        prompts.forEach { prompt ->
            row { index, count ->
                ListGroupRow(
                    index = index,
                    count = count,
                    title = prompt.title,
                    summary = prompt.subtitle,
                    icon = prompt.icon,
                    iconTint = MaterialTheme.colorScheme.onErrorContainer,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun MetadataRow(user: TwitterUser, ownerId: String?) {
    FlowRow(
        Modifier.padding(horizontal = ListGroupPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        user.professionalCategory?.takeIf { it.isNotBlank() }?.let {
            MetadataItem(Icons.Rounded.BusinessCenter, it)
        }
        user.location?.takeIf { it.isNotBlank() }?.let {
            MetadataItem(Icons.Rounded.LocationOn, it)
        }
        user.link?.takeIf { it.isNotBlank() }?.let { LinkItem(it) }
        formatBirthday(user.birthdateYear, user.birthdateMonth, user.birthdateDay)?.let {
            MetadataItem(Icons.Rounded.Cake, it)
        }
        user.joinedTime?.takeIf { it.isNotBlank() }?.let {
            MetadataItem(
                Icons.Rounded.CalendarMonth,
                stringResource(R.string.joined_format, formatJoinedTime(it)),
            )
        }
        if (ownerId != null && user.restId != ownerId) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Mail, null, Modifier.size(16.dp),
                    tint = if (user.canDm) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Rounded.Tag, null, Modifier.size(16.dp),
                    tint = if (user.canMediaTag) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
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
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LinkItem(url: String) {
    val primary = MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Link, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        SelectionContainer {
            Text(
                buildAnnotatedString {
                    withLink(LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = primary)))) {
                        append(url)
                    }
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
        StatItem(Icons.Rounded.Group, stringResource(R.string.following), user.followingCount),
        StatItem(Icons.Rounded.Group, stringResource(R.string.followers), user.followersCount),
        StatItem(Icons.Rounded.Create, stringResource(R.string.tweets), user.statusesCount),
        StatItem(Icons.Rounded.Image, stringResource(R.string.media_count), user.mediaCount),
        StatItem(Icons.Rounded.Favorite, stringResource(R.string.likes), user.favouritesCount),
        StatItem(Icons.AutoMirrored.Rounded.ListAlt, stringResource(R.string.listed_count), user.listedCount),
    )
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ListGroupPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3,
    ) {
        items.forEach { item ->
            Surface(
                Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        item.icon,
                        null,
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        item.value.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        softWrap = false,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 9.sp,
                            maxFontSize = 18.sp,
                            stepSize = 0.5.sp,
                        ),
                    )
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** A labelled, selectable identifier inside a list group. */
@Composable
private fun InfoRow(index: Int, count: Int, icon: ImageVector, title: String, value: String) {
    ListGroupItem(index, count) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            SelectionContainer {
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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
                .padding(horizontal = ListGroupPadding)
                .padding(bottom = ListGroupPadding),
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
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                SelectionContainer {
                    Text(
                        pretty,
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(pretty))
                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                ) {
                    Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
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
    AndroAlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) },
        title = { Text(stringResource(R.string.visit)) },
        text = {
            ListGroup(horizontalPadding = 0.dp) {
                if (screenName != null) {
                    row { index, count ->
                        ListGroupRow(
                            index = index,
                            count = count,
                            title = stringResource(R.string.open_in_app),
                            summary = "twitter://user?screen_name=$screenName",
                            icon = Icons.Rounded.Android,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            onClick = { open("twitter://user?screen_name=$screenName") },
                        )
                    }
                    row { index, count ->
                        ListGroupRow(
                            index = index,
                            count = count,
                            title = stringResource(R.string.browser_via_screen_name),
                            summary = "@$screenName",
                            icon = Icons.Rounded.AlternateEmail,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            onClick = { open("https://x.com/$screenName") },
                        )
                    }
                }
                if (user.restId.isNotEmpty()) {
                    row { index, count ->
                        ListGroupRow(
                            index = index,
                            count = count,
                            title = stringResource(R.string.browser_via_rest_id),
                            summary = "ID: ${user.restId}",
                            icon = Icons.Rounded.Fingerprint,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            onClick = { open("https://x.com/intent/user?user_id=${user.restId}") },
                        )
                    }
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
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        WavyCircularProgress()
                    }
                },
            )
        }
    }
}

/** Formats X's "EEE MMM dd HH:mm:ss Z yyyy" creation time in the device locale. */
internal fun formatJoinedTime(raw: String): String = runCatching {
    val parsed = SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.US).parse(raw)
    if (parsed == null) raw else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(parsed)
}.getOrDefault(raw)

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

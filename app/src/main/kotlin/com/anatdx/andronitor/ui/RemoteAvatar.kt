package com.anatdx.andronitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import java.io.File

@Composable
fun RemoteAvatar(url: String?, localPath: String? = null, size: Dp = 52.dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val model = localPath?.let { File(context.filesDir, it).takeIf(File::isFile) } ?: url
    val request = ImageRequest.Builder(context)
        .data(model)
        .crossfade(false)
        .build()
    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        modifier = modifier.size(size).clip(CircleShape),
        contentScale = ContentScale.Crop,
        loading = { AvatarPlaceholder(size) },
        error = { AvatarPlaceholder(size) },
    )
}

@Composable
private fun AvatarPlaceholder(size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text("@", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleMedium)
    }
}

/** Resolves the media model shown for a remote+local pair the way [RemoteAvatar] does. */
fun resolveMediaModel(context: android.content.Context, url: String?, localPath: String?): Any? =
    localPath?.let { File(context.filesDir, it).takeIf(File::isFile) } ?: url

@Composable
fun RemoteBanner(url: String?, localPath: String? = null, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val model = resolveMediaModel(context, url, localPath)
    if (model == null) {
        Box(modifier.background(MaterialTheme.colorScheme.outlineVariant))
        return
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context).data(model).crossfade(false).build(),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        loading = { Box(Modifier.background(MaterialTheme.colorScheme.outlineVariant)) },
        error = { Box(Modifier.background(MaterialTheme.colorScheme.outlineVariant)) },
    )
}

package com.anatdx.andronitor.data

import android.content.Context
import com.anatdx.andronitor.model.AppSettings
import com.anatdx.andronitor.model.MediaSaveScope
import com.anatdx.andronitor.model.TwitterUser
import com.anatdx.andronitor.network.HttpClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

class MediaHistoryService(
    private val context: Context,
    private val database: AndronitorDatabase,
) {
    private val directory: File by lazy {
        File(context.filesDir, "media_history").also { it.mkdirs() }
    }
    private val httpClient = HttpClientProvider.client(10_000, 30_000)

    suspend fun process(
        ownerId: String,
        users: Collection<TwitterUser>,
        followers: Set<String>,
        following: Set<String>,
        settings: AppSettings,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> },
    ): Int = withContext(Dispatchers.IO) {
        val candidates = users.flatMap { user ->
            val allowed = when (settings.mediaSaveScope) {
                MediaSaveScope.ALL -> true
                MediaSaveScope.FOLLOWING_ONLY -> user.restId in following
                MediaSaveScope.MUTUAL_ONLY -> user.restId in followers && user.restId in following
            }
            if (!allowed) return@flatMap emptyList()
            buildList {
                if (settings.saveAvatarHistory) user.avatarUrl?.takeIf { it.isNotBlank() }?.let { add("avatar" to it) }
                if (settings.saveBannerHistory) user.bannerUrl?.takeIf { it.isNotBlank() }?.let { add("banner" to it) }
            }
        }
        var completed = 0
        var downloaded = 0
        for ((type, remoteUrl) in candidates) {
            val existing = database.getMediaRecord(remoteUrl)
            val existingFile = existing?.let { File(context.filesDir, it.localFilePath) }
            if (existing != null && existingFile?.isFile == true && (!settings.avatarQualityHigh || existing.isHighQuality || type == "banner")) {
                completed++
                onProgress(completed, candidates.size, existing.localFilePath)
                continue
            }
            val effectiveUrl = if (type == "avatar") {
                val suffix = if (settings.avatarQualityHigh) "_400x400" else "_bigger"
                remoteUrl.replace(Regex("_(normal|bigger|400x400)"), suffix)
            } else remoteUrl
            val fileName = "${type}_${sha256(remoteUrl)}.${extension(remoteUrl)}"
            val target = File(directory, fileName)
            if (download(effectiveUrl, target)) {
                database.upsertMediaRecord(MediaRecord(0, type, "media_history/$fileName", remoteUrl, settings.avatarQualityHigh || type == "banner"))
                downloaded++
            }
            completed++
            onProgress(completed, candidates.size, fileName)
        }
        downloaded
    }

    private fun download(url: String, target: File): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Andronitor/1.0")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body ?: return false
                body.byteStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
                true
            }
        } catch (_: Exception) {
            target.delete()
            false
        }
    }

    private fun extension(url: String): String = url.substringBefore('?').substringAfterLast('.', "jpg")

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }
}

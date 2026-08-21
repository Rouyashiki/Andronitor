package com.anatdx.andronitor.data

import android.content.Context
import android.net.Uri
import com.anatdx.andronitor.model.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupService(
    private val context: Context,
    private val database: AndronitorDatabase,
    private val settingsStore: SettingsStore,
    private val cookies: SecureCookieStore,
) {
    suspend fun writeFullBackup(destination: Uri): Unit = withContext(Dispatchers.IO) {
        val temporary = File.createTempFile("andronitor_", ".zip", context.cacheDir)
        try {
            ZipOutputStream(temporary.outputStream().buffered()).use { zip ->
                writeText(zip, "manifest.json", JSONObject().apply { put("formatVersion", 1); put("app", "Andronitor") }.toString())
                writeText(zip, "settings.json", settingsStore.load().toJsonString())
                database.checkpointForBackup()
                val databaseFile = context.getDatabasePath("andronitor_data.db")
                if (databaseFile.isFile) writeFile(zip, databaseFile, "andronitor_data.db")
                val mediaRoot = File(context.filesDir, "media_history")
                if (mediaRoot.isDirectory) mediaRoot.walkTopDown().filter(File::isFile).forEach { file ->
                    writeFile(zip, file, "media_history/${file.relativeTo(mediaRoot).path.replace(File.separatorChar, '/')}")
                }
            }
            context.contentResolver.openOutputStream(destination)?.use { output -> temporary.inputStream().use { it.copyTo(output) } }
                ?: error("Unable to open the selected destination")
        } finally {
            temporary.delete()
        }
    }

    suspend fun restoreFullBackup(source: Uri): Unit = withContext(Dispatchers.IO) {
        val staging = File.createTempFile("andronitor_restore_", "", context.cacheDir).apply {
            delete()
            mkdirs()
        }
        try {
            context.contentResolver.openInputStream(source)?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val safeName = entry.name.replace('\\', '/')
                        require(!safeName.startsWith('/') && !safeName.contains("../") && safeName != "..") { "Unsafe backup path" }
                        val target = File(staging, safeName)
                        if (entry.isDirectory) target.mkdirs() else {
                            target.parentFile?.mkdirs()
                            target.outputStream().use { output -> zip.copyTo(output) }
                        }
                    }
                }
            } ?: error("Unable to read backup")
            val manifest = File(staging, "manifest.json")
            require(manifest.isFile && JSONObject(manifest.readText()).optInt("formatVersion") == 1) { "Unsupported Andronitor backup" }
            File(staging, "settings.json").takeIf(File::isFile)?.let { settingsStore.save(AppSettings.fromJsonString(it.readText())) }
            val databaseFile = File(staging, "andronitor_data.db")
            if (databaseFile.isFile) database.replaceFromBackup(databaseFile)
            val sourceMedia = File(staging, "media_history")
            if (sourceMedia.isDirectory) {
                val targetMedia = File(context.filesDir, "media_history")
                targetMedia.deleteRecursively()
                sourceMedia.copyRecursively(targetMedia, overwrite = true)
            }
            // Credentials are intentionally excluded from backups.
            cookies.deleteAllCookies()
            cookies.setActiveAccountId(null)
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun writeText(zip: ZipOutputStream, name: String, text: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(text.toByteArray())
        zip.closeEntry()
    }

    private fun writeFile(zip: ZipOutputStream, file: File, name: String) {
        zip.putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }
}

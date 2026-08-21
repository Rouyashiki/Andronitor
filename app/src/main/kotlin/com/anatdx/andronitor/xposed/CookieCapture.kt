package com.anatdx.andronitor.xposed

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.CookieManager
import java.util.Collections
import java.util.LinkedHashMap

/**
 * Framework-agnostic core of the X cookie hook. It reads the WebView cookie jar
 * for x.com / twitter.com and broadcasts a complete session cookie to Andronitor
 * exactly once per distinct value.
 *
 * Both entry points share this class — the classic rovo89 hook ([XCookieHook],
 * loaded on original Xposed / EdXposed / legacy LSPosed) and the modern libxposed
 * hook ([ModernCookieHook], loaded on modern LSPosed). Only the mechanism used to
 * install method hooks differs between them; the capture and broadcast logic here
 * is identical, so it lives in one place.
 *
 * A single process only ever runs one of the two entry points (the framework picks
 * exactly one), so one instance owns all capture state for that process.
 */
class CookieCapture(private val log: (String) -> Unit) {
    private val sent = Collections.synchronizedSet(HashSet<String>())
    @Volatile private var appContext: Context? = null

    /** Remember the first non-null application context we observe, for broadcasting. */
    fun rememberContext(context: Context?) {
        if (appContext == null && context != null) appContext = context
    }

    /** Read the current cookie jar and, if a complete new session is present, forward it. */
    fun tryCapture() {
        val cookie = readSessionCookie() ?: return
        if (!CookieBridge.isComplete(cookie)) return
        if (!sent.add(cookie)) return // already forwarded this exact value
        broadcast(cookie)
    }

    /** Merge the cookie jars for x.com and twitter.com into one cookie string. */
    private fun readSessionCookie(): String? {
        val manager = runCatching { CookieManager.getInstance() }.getOrNull() ?: return null
        val merged = LinkedHashMap<String, String>()
        for (url in CookieBridge.COOKIE_URLS) {
            val raw = runCatching { manager.getCookie(url) }.getOrNull().orEmpty()
            raw.split(';').forEach { part ->
                val key = part.substringBefore('=', "").trim()
                val value = part.substringAfter('=', "").trim()
                if (key.isNotEmpty() && value.isNotEmpty()) merged[key] = value
            }
        }
        if (merged.isEmpty()) return null
        return merged.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    private fun broadcast(cookie: String) {
        val context = appContext
        if (context == null) {
            log("captured a cookie but have no Context yet; will retry on next event")
            sent.remove(cookie)
            return
        }
        val intent = Intent(CookieBridge.ACTION_COOKIE).apply {
            // Explicit component (not just setPackage): required on aggressive
            // OEM ROMs to start Andronitor's process when it isn't running.
            component = ComponentName(
                CookieBridge.ANDRONITOR_PACKAGE,
                "${CookieBridge.ANDRONITOR_PACKAGE}.CookieReceiver",
            )
            putExtra(CookieBridge.EXTRA_COOKIE, cookie)
            putExtra(CookieBridge.EXTRA_SECRET, CookieBridge.SECRET)
            putExtra(CookieBridge.EXTRA_SOURCE, Build.VERSION.SDK_INT.toLong())
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
        runCatching { context.sendBroadcast(intent) }
            .onSuccess { log("forwarded X session cookie to Andronitor") }
            .onFailure {
                log("broadcast failed: ${it.message}")
                sent.remove(cookie)
            }
    }
}

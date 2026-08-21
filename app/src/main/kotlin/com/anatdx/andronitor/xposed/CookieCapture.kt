package com.anatdx.andronitor.xposed

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
    @Volatile private var accountsReported = false

    /** Remember the first non-null application context we observe, for broadcasting. */
    fun rememberContext(context: Context?) {
        if (appContext == null && context != null) appContext = context
    }

    /**
     * Report the X app's account list to Andronitor once, so it can show the user
     * which accounts exist and which still need importing. Identities only — no
     * tokens. Independent of cookie capture: it fires even when the active account
     * has no web session yet.
     */
    fun tryReportAccounts() {
        if (accountsReported) return
        val context = appContext ?: return
        val json = XAccountEnumerator.enumerateJson(context) ?: return
        accountsReported = true
        val ok = send(context, CookieBridge.ACTION_ACCOUNTS) {
            putExtra(CookieBridge.EXTRA_ACCOUNTS, json)
        }
        if (ok) log("reported X account list to Andronitor") else accountsReported = false
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
        val ok = send(context, CookieBridge.ACTION_COOKIE) {
            putExtra(CookieBridge.EXTRA_COOKIE, cookie)
            putExtra(CookieBridge.EXTRA_SOURCE, Build.VERSION.SDK_INT.toLong())
        }
        if (ok) log("forwarded X session cookie to Andronitor") else sent.remove(cookie)
    }

    /**
     * Send [action] to Andronitor's CookieReceiver, guarded by the shared secret.
     * Uses an explicit component (not just setPackage) because aggressive OEM ROMs
     * otherwise won't start Andronitor's process for the broadcast.
     */
    private inline fun send(context: Context, action: String, extras: Intent.() -> Unit): Boolean {
        val intent = Intent(action).apply {
            component = ComponentName(
                CookieBridge.ANDRONITOR_PACKAGE,
                "${CookieBridge.ANDRONITOR_PACKAGE}.CookieReceiver",
            )
            putExtra(CookieBridge.EXTRA_SECRET, CookieBridge.SECRET)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            extras()
        }
        return runCatching { context.sendBroadcast(intent) }
            .onFailure { log("broadcast ($action) failed: ${it.message}") }
            .isSuccess
    }

    /**
     * Snapshot the "already forwarded" latches so a hot-reloaded generation does
     * not re-broadcast a cookie or account list the previous one already sent.
     *
     * Everything stored here is a bootclasspath type, never an object created
     * under the module classloader, so handing it to the next generation cannot
     * keep the retired one strongly reachable. The Context is deliberately left
     * behind: the new generation re-acquires it from its own hooks.
     */
    fun saveState(): Bundle = Bundle().apply {
        putStringArrayList(KEY_SENT, ArrayList(sent))
        putBoolean(KEY_ACCOUNTS_REPORTED, accountsReported)
    }

    /** Adopt the state [saveState] captured in the previous generation, if any. */
    fun restoreState(state: Any?) {
        val bundle = state as? Bundle ?: return
        bundle.getStringArrayList(KEY_SENT)?.let(sent::addAll)
        if (bundle.getBoolean(KEY_ACCOUNTS_REPORTED)) accountsReported = true
    }

    private companion object {
        const val KEY_SENT = "sent_cookies"
        const val KEY_ACCOUNTS_REPORTED = "accounts_reported"
    }
}

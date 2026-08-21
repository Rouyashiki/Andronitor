package com.anatdx.andronitor.xposed

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.CookieManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Xposed entry point. Loaded into every hooked process; only arms itself inside
 * [CookieBridge.TARGET_PACKAGE]. It reads the framework [CookieManager] cookie
 * jar for x.com — a stable, non-obfuscated Android API — and, once all required
 * session cookies are present, broadcasts them to Andronitor exactly once per
 * distinct value.
 *
 * Note on feasibility: the X app authenticates natively with OAuth 1.0a and only
 * populates the WebView cookie jar when it renders x.com web content (login,
 * some settings/help flows). If the user never triggers such a flow, no web
 * cookie exists and nothing is sent — Andronitor reports the pending state
 * rather than failing.
 */
class XCookieHook : IXposedHookLoadPackage {

    private val sent = java.util.Collections.synchronizedSet(HashSet<String>())
    @Volatile private var appContext: Context? = null

    private val cookieHookInstalled = java.util.concurrent.atomic.AtomicBoolean(false)

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != CookieBridge.TARGET_PACKAGE) return
        log("armed in ${lpparam.packageName}")

        captureApplicationContext()
        hookActivityResume()
        // The setCookie hook is installed lazily (see installCookieWriteHook):
        // at handleLoadPackage time the WebView provider isn't up yet, so
        // CookieManager.getInstance() is unavailable.
    }

    /** Grab a Context as soon as the target Application is created. */
    private fun captureApplicationContext() {
        XposedHelpers.findAndHookMethod(
            Application::class.java,
            "attach",
            Context::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    appContext = (param.thisObject as? Application)?.applicationContext
                        ?: param.args.getOrNull(0) as? Context
                }
            },
        )
    }

    /**
     * Install a hook on every `setCookie` overload of the concrete CookieManager
     * so a write forwards immediately (no need to wait for the next resume).
     *
     * `android.webkit.CookieManager.setCookie` is abstract; the real code lives
     * in the WebView provider's concrete subclass (e.g. CookieManagerAdapter).
     * The provider only comes up once WebView is touched, so this is called
     * lazily from the resume hook — guarded to run exactly once.
     */
    private fun installCookieWriteHook() {
        if (cookieHookInstalled.get()) return
        val concrete = runCatching { CookieManager.getInstance()::class.java }.getOrNull() ?: return
        if (!cookieHookInstalled.compareAndSet(false, true)) return
        val callback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) = tryCapture()
        }
        val hooked = runCatching {
            XposedBridge.hookAllMethods(concrete, "setCookie", callback).size
        }.getOrElse {
            log("setCookie hook failed on ${concrete.name}: ${it.message}")
            cookieHookInstalled.set(false)
            0
        }
        if (hooked > 0) log("hooked $hooked setCookie overload(s) on ${concrete.name}")
    }

    /**
     * Poll on each Activity resume: covers cookies set before we armed, and is
     * where we lazily install the setCookie write hook once WebView is up.
     */
    private fun hookActivityResume() {
        runCatching {
            XposedHelpers.findAndHookMethod(
                Activity::class.java,
                "onResume",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (appContext == null) {
                            appContext = (param.thisObject as? Activity)?.applicationContext
                        }
                        installCookieWriteHook()
                        tryCapture()
                    }
                },
            )
        }.onFailure { log("onResume hook failed: ${it.message}") }
    }

    private fun tryCapture() {
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
            component = android.content.ComponentName(
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

    private fun log(message: String) = XposedBridge.log("[Andronitor] $message")
}

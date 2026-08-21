package com.anatdx.andronitor.xposed

import android.app.Activity
import android.app.Application
import android.content.Context
import android.webkit.CookieManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Classic (rovo89 API 82) Xposed entry point. Loaded on original Xposed, EdXposed,
 * and legacy LSPosed via `assets/xposed_init`. Modern LSPosed ignores this class and
 * loads [ModernCookieHook] instead (see `META-INF/xposed/java_init.list`); the two
 * entries are mutually exclusive per process, so hooks are never installed twice.
 *
 * It only arms itself inside [CookieBridge.TARGET_PACKAGE]. The actual reading of the
 * [CookieManager] cookie jar and the broadcast to Andronitor live in the shared,
 * framework-agnostic [CookieCapture]; this class only wires up the classic hooks.
 *
 * Note on feasibility: the X app authenticates natively with OAuth 1.0a and only
 * populates the WebView cookie jar when it renders x.com web content (login,
 * some settings/help flows). If the user never triggers such a flow, no web
 * cookie exists and nothing is sent — Andronitor reports the pending state
 * rather than failing.
 */
class XCookieHook : IXposedHookLoadPackage {

    private val capture = CookieCapture(::log)
    private val cookieHookInstalled = AtomicBoolean(false)

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
                    capture.rememberContext(
                        (param.thisObject as? Application)?.applicationContext
                            ?: param.args.getOrNull(0) as? Context,
                    )
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
            override fun afterHookedMethod(param: MethodHookParam) = capture.tryCapture()
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
                        capture.rememberContext((param.thisObject as? Activity)?.applicationContext)
                        capture.tryReportAccounts()
                        installCookieWriteHook()
                        capture.tryCapture()
                    }
                },
            )
        }.onFailure { log("onResume hook failed: ${it.message}") }
    }

    private fun log(message: String) = XposedBridge.log("[Andronitor] $message")
}

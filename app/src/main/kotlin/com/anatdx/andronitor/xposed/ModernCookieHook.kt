package com.anatdx.andronitor.xposed

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Modern (libxposed) Xposed entry point, loaded on modern LSPosed via
 * `META-INF/xposed/java_init.list`. It performs the same capture as the classic
 * [XCookieHook] — reading the X WebView cookie jar and broadcasting a complete
 * session cookie to Andronitor through the shared [CookieCapture] — but installs
 * its hooks with the libxposed interceptor-chain API instead of the rovo89 API.
 *
 * Modern LSPosed reads `java_init.list` and never touches `assets/xposed_init`, so
 * only one of the two entry points is ever active in a given process — the hooks
 * are never installed twice. Legacy frameworks that don't understand the modern
 * entry fall back to the classic one.
 *
 * The module declares a static scope of the X app only (see
 * `META-INF/xposed/scope.list` and `staticScope` in `module.prop`), so this class
 * is only ever loaded into [CookieBridge.TARGET_PACKAGE]; the package guard below
 * is belt-and-braces.
 */
class ModernCookieHook : XposedModule() {

    private val capture = CookieCapture(::log)
    private val armed = AtomicBoolean(false)
    private val cookieHookInstalled = AtomicBoolean(false)

    // onPackageLoaded is @RequiresApi(Q); onPackageReady covers API 28. Whichever
    // the framework delivers first arms the hooks — arm() is gated so the other,
    // and any repeat delivery, is a no-op (no double hooking).
    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != CookieBridge.TARGET_PACKAGE) return
        arm()
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != CookieBridge.TARGET_PACKAGE) return
        arm()
    }

    /**
     * Consent to being retired so the framework can hot reload us; the default
     * implementation returns false and refuses. Nothing needs tearing down
     * first: [CookieCapture] owns no threads, native hooks or registered
     * callbacks, only a dedupe set and a reference to the target's Context, and
     * the framework itself retires the Java hooks handed to [onHotReloaded].
     */
    override fun onHotReloading(param: HotReloadingParam): Boolean = true

    /**
     * Re-install hooks after a hot reload so an updated module keeps working
     * without force-stopping the X app. The framework hands us the previous
     * generation's handles; we drop them and hook afresh. Application.attach has
     * already fired in the running process, but the Activity.onResume hook below
     * re-installs the setCookie hook and resumes capture on the next resume.
     */
    override fun onHotReloaded(param: HotReloadedParam) {
        param.oldHookHandles.forEach { runCatching { it.unhook() } }
        armed.set(false)
        cookieHookInstalled.set(false)
        arm()
    }

    private fun arm() {
        if (!armed.compareAndSet(false, true)) return
        log("armed in ${CookieBridge.TARGET_PACKAGE}")
        hookApplicationAttach()
        hookActivityResume()
        // setCookie is hooked lazily once WebView is up (see installCookieWriteHook).
    }

    /** Grab a Context as soon as the target Application is created. */
    private fun hookApplicationAttach() {
        val attach = runCatching {
            Application::class.java.getDeclaredMethod("attach", Context::class.java)
        }.getOrNull() ?: return
        runCatching {
            hook(attach).intercept(afterCall { chain ->
                capture.rememberContext(
                    (chain.thisObject as? Application)?.applicationContext
                        ?: chain.args.getOrNull(0) as? Context,
                )
            })
        }.onFailure { log("attach hook failed: ${it.message}") }
    }

    /**
     * Poll on each Activity resume: covers cookies set before we armed, and is
     * where we lazily install the setCookie write hook once WebView is up.
     */
    private fun hookActivityResume() {
        val onResume = runCatching {
            Activity::class.java.getDeclaredMethod("onResume")
        }.getOrNull() ?: return
        runCatching {
            hook(onResume).intercept(afterCall { chain ->
                capture.rememberContext((chain.thisObject as? Activity)?.applicationContext)
                capture.tryReportAccounts()
                installCookieWriteHook()
                capture.tryCapture()
            })
        }.onFailure { log("onResume hook failed: ${it.message}") }
    }

    /**
     * Hook every `setCookie` overload of the concrete CookieManager so a write
     * forwards immediately. The concrete provider only exists once WebView is up,
     * so this runs lazily from the resume hook, guarded to install exactly once.
     */
    private fun installCookieWriteHook() {
        if (cookieHookInstalled.get()) return
        val concrete = runCatching { CookieManager.getInstance()::class.java }.getOrNull() ?: return
        if (!cookieHookInstalled.compareAndSet(false, true)) return
        val overloads = runCatching {
            concrete.methods.filter { it.name == "setCookie" && !it.isSynthetic }
        }.getOrElse { emptyList() }
        var hooked = 0
        for (method in overloads) {
            runCatching {
                hook(method).intercept(afterCall { capture.tryCapture() })
                hooked++
            }.onFailure { log("setCookie hook failed on $method: ${it.message}") }
        }
        if (hooked > 0) log("hooked $hooked setCookie overload(s) on ${concrete.name}")
        else cookieHookInstalled.set(false)
    }

    /**
     * Adapt an "after the original runs" action to the libxposed interceptor chain:
     * proceed with the original call, then run [action]; the action never changes
     * the return value or masks an exception thrown by the callee.
     */
    private inline fun afterCall(
        crossinline action: (XposedInterface.Chain) -> Unit,
    ): XposedInterface.Hooker = XposedInterface.Hooker { chain ->
        val result = chain.proceed()
        runCatching { action(chain) }
        result
    }

    private fun log(message: String) = log(Log.INFO, TAG, "[Andronitor] $message")

    private companion object {
        const val TAG = "Andronitor"
    }
}

package com.anatdx.andronitor.xposed

import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges the Andronitor app process to the modern LSPosed framework service.
 *
 * On modern LSPosed the framework binds a service to the module app shortly after
 * launch (delivered via the content provider the libxposed:service library merges
 * into our manifest). We hold that service so the UI can ask the framework to add
 * the X app to this module's scope on demand — see the import-from-X flow.
 *
 * On classic frameworks, or when no Xposed framework is active at all, no service
 * ever binds and [service] stays null. Every caller must degrade gracefully in
 * that case (the classic entry point and manual cookie paths still work).
 */
object XposedServiceConnection {
    private val _service = MutableStateFlow<XposedService?>(null)

    /** The bound framework service, or null when running without modern LSPosed. */
    val service: StateFlow<XposedService?> = _service.asStateFlow()

    @Volatile private var registered = false

    /** Register once (from Application.onCreate) to receive the framework service. */
    fun init() {
        if (registered) return
        registered = true
        // registerListener must be called at most once; guarded above.
        runCatching {
            XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
                override fun onServiceBind(service: XposedService) {
                    _service.value = service
                }

                override fun onServiceDied(service: XposedService) {
                    if (_service.value === service) _service.value = null
                }
            })
        }
    }

    /**
     * Ask the framework to add the X app to this module's scope, if a modern
     * framework service is bound and the X app is not already scoped. No-op on
     * classic frameworks. Fire-and-forget: approval may require user confirmation
     * in the LSPosed manager, after which the hook loads into the X app.
     */
    fun requestXScope() {
        val svc = _service.value ?: return
        runCatching {
            val alreadyScoped = runCatching { svc.scope }.getOrNull()
                ?.contains(CookieBridge.TARGET_PACKAGE) == true
            if (alreadyScoped) return
            svc.requestScope(
                listOf(CookieBridge.TARGET_PACKAGE),
                object : XposedService.OnScopeEventListener {
                    override fun onScopeRequestApproved(approved: MutableList<String>) {}
                    override fun onScopeRequestFailed(message: String) {}
                },
            )
        }
    }
}

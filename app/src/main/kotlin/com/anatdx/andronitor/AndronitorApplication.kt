package com.anatdx.andronitor

import android.app.Application
import com.anatdx.andronitor.xposed.XposedServiceConnection

/**
 * Application entry point. Registers the modern LSPosed service listener as early
 * as possible so the framework service is available by the time the user reaches
 * the import-from-X flow. Registration is a no-op when no modern framework is
 * present, so this is safe for ordinary (non-Xposed) installs.
 */
class AndronitorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        XposedServiceConnection.init()
    }
}

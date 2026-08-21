package com.anatdx.andronitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anatdx.andronitor.xposed.CookieBridge
import com.anatdx.andronitor.xposed.CookieInbox
import com.anatdx.andronitor.xposed.PendingCookieStore

/**
 * Receives X session cookies broadcast by the Xposed hook running inside the X
 * app's process. Validates the shared secret and completeness before handing the
 * cookie to [CookieInbox], which the ViewModel drains into `addAccount`.
 *
 * Declared exported (the sender lives in another app / UID) but the sender
 * addresses us by package and includes the secret, so arbitrary apps cannot
 * inject a cookie.
 */
class CookieReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CookieBridge.ACTION_COOKIE) return
        if (intent.getStringExtra(CookieBridge.EXTRA_SECRET) != CookieBridge.SECRET) return
        val cookie = intent.getStringExtra(CookieBridge.EXTRA_COOKIE)?.trim().orEmpty()
        if (cookie.isEmpty() || !CookieBridge.isComplete(cookie)) return
        // Persist first so the cookie survives if this process (started only to
        // run the receiver) is killed before the ViewModel consumes it...
        PendingCookieStore.put(context, cookie)
        // ...then also notify any already-running collector for instant import.
        CookieInbox.offer(cookie)
    }
}

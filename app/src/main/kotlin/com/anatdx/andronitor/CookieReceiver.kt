package com.anatdx.andronitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anatdx.andronitor.xposed.CookieBridge
import com.anatdx.andronitor.xposed.CookieInbox
import com.anatdx.andronitor.xposed.DiscoveredAccountsInbox
import com.anatdx.andronitor.xposed.DiscoveredXAccount
import com.anatdx.andronitor.xposed.PendingCookieStore
import org.json.JSONArray

/**
 * Receives broadcasts from the Xposed hook running inside the X app's process:
 * captured session cookies ([CookieBridge.ACTION_COOKIE]) and the list of X
 * accounts the hook found ([CookieBridge.ACTION_ACCOUNTS]). Validates the shared
 * secret on both before trusting anything.
 *
 * Declared exported (the sender lives in another app / UID) but the sender
 * addresses us by component and includes the secret, so arbitrary apps cannot
 * inject data.
 */
class CookieReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getStringExtra(CookieBridge.EXTRA_SECRET) != CookieBridge.SECRET) return
        when (intent.action) {
            CookieBridge.ACTION_COOKIE -> handleCookie(context, intent)
            CookieBridge.ACTION_ACCOUNTS -> handleAccounts(intent)
        }
    }

    private fun handleCookie(context: Context, intent: Intent) {
        val cookie = intent.getStringExtra(CookieBridge.EXTRA_COOKIE)?.trim().orEmpty()
        if (cookie.isEmpty() || !CookieBridge.isComplete(cookie)) return
        // Persist first so the cookie survives if this process (started only to
        // run the receiver) is killed before the ViewModel consumes it...
        PendingCookieStore.put(context, cookie)
        // ...then also notify any already-running collector for instant import.
        CookieInbox.offer(cookie)
    }

    private fun handleAccounts(intent: Intent) {
        val json = intent.getStringExtra(CookieBridge.EXTRA_ACCOUNTS)?.trim().orEmpty()
        if (json.isEmpty()) return
        val parsed = runCatching {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val id = obj.optString("id").trim()
                val screenName = obj.optString("screenName").trim()
                if (id.isEmpty() && screenName.isEmpty()) null
                else DiscoveredXAccount(id, screenName)
            }
        }.getOrNull() ?: return
        if (parsed.isNotEmpty()) DiscoveredAccountsInbox.offer(parsed)
    }
}

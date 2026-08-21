package com.anatdx.andronitor.xposed

import android.content.Context

/**
 * Durable hand-off point for cookies delivered by [com.anatdx.andronitor.CookieReceiver].
 *
 * A broadcast can start Andronitor's process just to run the receiver, with no
 * UI and no ViewModel subscribed; that process may then be frozen or killed
 * before anything consumes an in-memory value. Writing to SharedPreferences
 * survives that, so the ViewModel can drain whatever arrived while it was gone
 * on its next start — in addition to the live [CookieInbox] flow for the
 * already-running case.
 */
object PendingCookieStore {
    private const val PREFS = "pending_cookies"
    private const val KEY = "pending_cookie"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Persist the latest complete cookie awaiting import. Uses commit() so the
     * write is on disk before the (possibly short-lived) receiver process dies.
     */
    fun put(context: Context, cookie: String) {
        prefs(context).edit().putString(KEY, cookie).commit()
    }

    /**
     * Return and clear any pending cookie. Uses commit() for the removal so a
     * drained cookie can't be re-imported on the next launch if the process
     * dies right after draining.
     */
    fun take(context: Context): String? {
        val p = prefs(context)
        val cookie = p.getString(KEY, null)
        if (cookie != null) p.edit().remove(KEY).commit()
        return cookie
    }
}

package com.anatdx.andronitor.xposed

/**
 * Constants shared between the Xposed hook (running inside the X app's process)
 * and Andronitor's own process. Because the module ships inside the Andronitor
 * APK, both sides compile against the exact same values.
 *
 * The hook runs under the target app's UID, so it cannot hold an Andronitor
 * signature-level permission. Instead the broadcast is (a) explicitly addressed
 * to Andronitor's package via setPackage, so it never fans out to other apps,
 * and (b) tagged with a build-time [SECRET] the receiver verifies before
 * trusting the payload.
 */
object CookieBridge {
    /** Andronitor's own application id — the hook targets this package explicitly. */
    const val ANDRONITOR_PACKAGE = "com.anatdx.andronitor"

    /** The only process we hook. */
    const val TARGET_PACKAGE = "com.twitter.android"

    /** Broadcast action carrying a captured X web-session cookie. */
    const val ACTION_COOKIE = "com.anatdx.andronitor.action.X_COOKIE"

    /** Broadcast action carrying the list of X accounts found in the X app. */
    const val ACTION_ACCOUNTS = "com.anatdx.andronitor.action.X_ACCOUNTS"

    /** String extra: the full `k=v; k=v` cookie string for x.com. */
    const val EXTRA_COOKIE = "cookie"

    /** String extra: JSON array `[{"id":..,"screenName":..}]` of discovered accounts. */
    const val EXTRA_ACCOUNTS = "accounts"

    /** Android AccountManager account type registered by the X app. */
    const val X_ACCOUNT_TYPE = "com.twitter.android.auth.login"

    /** AccountManager userdata key holding an account's numeric X user id. */
    const val X_ACCOUNT_USER_ID_KEY = "account_user_id"

    /** String extra: shared secret guarding the broadcast. */
    const val EXTRA_SECRET = "secret"

    /** Long extra: SDK level the hook observed, for diagnostics. */
    const val EXTRA_SOURCE = "source"

    /**
     * Shared secret. Not a cryptographic boundary — the payload is a cookie the
     * user already owns — but it stops unrelated apps from spoofing an import by
     * broadcasting to us. Rotate by changing this constant.
     */
    const val SECRET = "andronitor-x-cookie-bridge-v1"

    /** Cookies that together prove a usable web session. */
    val REQUIRED_KEYS = listOf("auth_token", "ct0", "twid")

    /** Domains whose cookie jar we read inside the X process. */
    val COOKIE_URLS = listOf("https://x.com", "https://twitter.com")

    /** True when [cookie] contains every key the Andronitor API layer needs. */
    fun isComplete(cookie: String): Boolean {
        val keys = cookie.split(';')
            .mapNotNull { it.substringBefore('=', "").trim().takeIf(String::isNotEmpty) }
            .toSet()
        return REQUIRED_KEYS.all { it in keys }
    }
}

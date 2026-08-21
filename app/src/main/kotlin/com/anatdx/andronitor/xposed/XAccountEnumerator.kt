package com.anatdx.andronitor.xposed

import android.accounts.AccountManager
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Enumerates the X accounts logged into the X app, read from Android's
 * [AccountManager] inside the X process.
 *
 * The hook runs under the X app's uid, which owns these accounts (X is their
 * authenticator), so it can read the account list and each account's numeric
 * user-id userdata without holding GET_ACCOUNTS or any other permission.
 *
 * This reads identities only — screen name + numeric user id (both public) —
 * and never touches the OAuth tokens stored alongside them. Its purpose is to
 * let Andronitor show the user which accounts exist and which still need a
 * cookie; the actual web session is still captured per active account by the
 * cookie hook.
 */
object XAccountEnumerator {
    /**
     * Return the discovered accounts as a JSON array string
     * `[{"id":"..","screenName":".."}]`, or null if none / unreadable.
     */
    fun enumerateJson(context: Context): String? {
        val am = AccountManager.get(context)
        val accounts = runCatching { am.getAccountsByType(CookieBridge.X_ACCOUNT_TYPE) }
            .getOrNull() ?: return null
        if (accounts.isEmpty()) return null
        val array = JSONArray()
        for (account in accounts) {
            val id = runCatching { am.getUserData(account, CookieBridge.X_ACCOUNT_USER_ID_KEY) }
                .getOrNull()?.trim().orEmpty()
            val screenName = account.name?.trim().orEmpty()
            if (id.isEmpty() && screenName.isEmpty()) continue
            array.put(
                JSONObject().apply {
                    put("id", id)
                    put("screenName", screenName)
                },
            )
        }
        return if (array.length() == 0) null else array.toString()
    }
}

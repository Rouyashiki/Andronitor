package com.anatdx.andronitor.xposed

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** An X account the hook found in the X app: numeric user id + screen handle. */
data class DiscoveredXAccount(val id: String, val screenName: String)

/**
 * Latest list of X accounts reported by the hook (via [com.anatdx.andronitor.CookieReceiver]).
 * Empty until the hook reports one. The ViewModel observes this to show the user
 * which accounts exist in the X app and which still need a cookie import.
 */
object DiscoveredAccountsInbox {
    private val _accounts = MutableStateFlow<List<DiscoveredXAccount>>(emptyList())
    val accounts: StateFlow<List<DiscoveredXAccount>> = _accounts.asStateFlow()

    fun offer(list: List<DiscoveredXAccount>) {
        _accounts.value = list
    }
}

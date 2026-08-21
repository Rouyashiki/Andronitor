package com.anatdx.andronitor.xposed

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-process channel from [CookieReceiver] to the ViewModel. A replay buffer of
 * 1 means a cookie delivered while no collector is attached (app launched cold
 * by the broadcast) is still handed to the first collector that subscribes.
 */
object CookieInbox {
    private val _cookies = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 4,
    )
    val cookies: SharedFlow<String> = _cookies.asSharedFlow()

    fun offer(cookie: String) {
        _cookies.tryEmit(cookie)
    }
}

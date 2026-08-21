package com.anatdx.andronitor.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DomainModelsTest {
    @Test
    fun twitterUserRoundTripsThroughMoshi() {
        val original = TwitterUser(
            restId = "123",
            name = "Alice",
            screenName = "alice",
            followersCount = 42,
            isVerified = true,
            isFollower = true,
        )

        assertEquals(original, TwitterUser.fromJsonString(original.toJsonString()))
    }

    @Test
    fun settingsRoundTripKeepsEnumsAndMap() {
        val original = AppSettings(
            mediaSaveScope = MediaSaveScope.MUTUAL_ONLY,
            syncFetchMode = SyncFetchMode.FASTEST,
            customQueryIds = mapOf("Following" to "query-id"),
            historyLimit = 9,
        )

        assertEquals(original, AppSettings.fromJsonString(original.toJsonString()))
    }
}

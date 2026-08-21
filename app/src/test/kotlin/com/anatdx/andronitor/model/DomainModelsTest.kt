package com.anatdx.andronitor.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
            bioLinks = listOf("https://example.com"),
            pinnedTweetId = "456",
            parodyLabel = "Parody",
            birthdateYear = "2000",
            birthdateMonth = "5",
            birthdateDay = "12",
            automatedScreenName = "botmaster",
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

    @Test
    fun fromGraphQlParsesLegacyShapeWithRichFields() {
        val raw = JSONObject(
            """
            {
              "result": {
                "rest_id": "2033116026410598400",
                "is_blue_verified": true,
                "core": {"name": "Irosas", "screen_name": "Irosas_xyn", "created_at": "Sun Mar 15 17:40:49 +0000 2026"},
                "legacy": {
                  "description": "bio with https://t.co/abc123 inside",
                  "entities": {
                    "description": {"urls": [{"url": "https://t.co/abc123", "expanded_url": "http://telegram.me/irosas_xyn"}]},
                    "url": {"urls": [{"expanded_url": "https://bsky.app/profile/irosas.bsky.social"}]}
                  },
                  "location": "Jinan",
                  "profile_image_url_https": "https://pbs.twimg.com/profile_images/x_normal.jpg",
                  "profile_banner_url": "https://pbs.twimg.com/banners/x",
                  "followers_count": 1310,
                  "friends_count": 413,
                  "statuses_count": 1961,
                  "media_count": 101,
                  "favourites_count": 2403,
                  "listed_count": 0,
                  "pinned_tweet_ids_str": ["2089030794274816442"],
                  "can_dm": true,
                  "can_media_tag": false,
                  "protected": false
                },
                "legacy_extended_profile": {"birthdate": {"year": 2011, "month": 5, "day": 12}},
                "professional": {"category": [{"name": "Artist", "display": true}]},
                "parody_commentary_fan_label": "None",
                "relationship_perspectives": {"following": true, "followed_by": true}
              }
            }
            """.trimIndent(),
        )

        val user = TwitterUser.fromGraphQl(raw)

        assertEquals("2033116026410598400", user.restId)
        assertEquals("Irosas", user.name)
        assertEquals("Irosas_xyn", user.screenName)
        assertEquals("bio with http://telegram.me/irosas_xyn inside", user.bio)
        assertEquals(listOf("http://telegram.me/irosas_xyn"), user.bioLinks)
        assertEquals("https://bsky.app/profile/irosas.bsky.social", user.link)
        assertEquals("Jinan", user.location)
        assertEquals("Sun Mar 15 17:40:49 +0000 2026", user.joinedTime)
        assertEquals("2089030794274816442", user.pinnedTweetId)
        assertEquals("2011", user.birthdateYear)
        assertEquals("5", user.birthdateMonth)
        assertEquals("12", user.birthdateDay)
        assertEquals("Artist", user.professionalCategory)
        assertEquals("None", user.parodyLabel)
        assertEquals(1310, user.followersCount)
        assertEquals(413, user.followingCount)
        assertEquals(1961, user.statusesCount)
        assertEquals(101, user.mediaCount)
        assertEquals(2403, user.favouritesCount)
        assertEquals(0, user.listedCount)
        assertTrue(user.isVerified)
        assertTrue(user.isFollower)
        assertTrue(user.isFollowing)
        assertTrue(user.canDm)
        assertEquals(false, user.canMediaTag)
    }

    @Test
    fun fromGraphQlParsesModernShapeFallbacks() {
        val raw = JSONObject(
            """
            {
              "result": {
                "rest_id": "99",
                "core": {"name": "Hiner", "screen_name": "hiner233"},
                "legacy": {},
                "avatar": {"image_url": "https://pbs.twimg.com/profile_images/99_normal.jpg"},
                "banner": {"image_url": "https://pbs.twimg.com/banners/99"},
                "location": {"location": "Tokyo"},
                "website": {"url": "https://hiner.example"},
                "profile_bio": {
                  "description": "是 Hiner https://t.co/zzz",
                  "entities": {"description": {"urls": [{"url": "https://t.co/zzz", "expanded_url": "https://nekopura.example"}]}}
                },
                "relationship_counts": {"followers": 2095, "following": 689},
                "tweet_counts": {"tweets": 100, "media_tweets": 7},
                "action_counts": {"favorites_count": 55},
                "dm_permissions": {"can_dm": true},
                "media_permissions": {"can_media_tag": true},
                "privacy": {"protected": true},
                "verification": {"verified": true},
                "pinned_items": {"tweet_ids_str": ["777"]}
              }
            }
            """.trimIndent(),
        )

        val user = TwitterUser.fromGraphQl(raw)

        assertEquals("99", user.restId)
        assertEquals("https://pbs.twimg.com/profile_images/99_normal.jpg", user.avatarUrl)
        assertEquals("https://pbs.twimg.com/banners/99", user.bannerUrl)
        assertEquals("Tokyo", user.location)
        assertEquals("https://hiner.example", user.link)
        assertEquals("是 Hiner https://nekopura.example", user.bio)
        assertEquals(2095, user.followersCount)
        assertEquals(689, user.followingCount)
        assertEquals(100, user.statusesCount)
        assertEquals(7, user.mediaCount)
        assertEquals(55, user.favouritesCount)
        assertEquals("777", user.pinnedTweetId)
        assertTrue(user.canDm)
        assertTrue(user.canMediaTag)
        assertTrue(user.isProtected)
        assertTrue(user.isVerified)
    }

    @Test
    fun fromGraphQlDetectsSuspendedUser() {
        val raw = JSONObject(
            """{"result": {"__typename": "UserUnavailable", "message": "User is suspended", "rest_id": "1"}}""",
        )

        val user = TwitterUser.fromGraphQl(raw)

        assertEquals("suspended", user.status)
    }

    @Test
    fun fromJsonStringUnwrapsLegacyLocationObject() {
        val stored = """{"rest_id": "7", "screen_name": "x", "location": "{\"location\":\"Taiwan\"}"}"""

        assertEquals("Taiwan", TwitterUser.fromJsonString(stored).location)
    }

    @Test
    fun fromV1ExpandsBioLinks() {
        val raw = JSONObject(
            """
            {
              "id_str": "5",
              "screen_name": "v1user",
              "description": "check https://t.co/short out",
              "entities": {
                "description": {"urls": [{"url": "https://t.co/short", "expanded_url": "https://long.example/page"}]},
                "url": {"urls": [{"expanded_url": "https://site.example"}]}
              }
            }
            """.trimIndent(),
        )

        val user = TwitterUser.fromV1(raw)

        assertEquals("check https://long.example/page out", user.bio)
        assertEquals(listOf("https://long.example/page"), user.bioLinks)
        assertEquals("https://site.example", user.link)
        assertNull(user.pinnedTweetId)
    }

    @Test
    fun fromV1FallsBackToRawUrlWhenEntityLacksExpandedUrl() {
        val raw = JSONObject(
            """
            {
              "id_str": "6",
              "screen_name": "legacyurl",
              "url": "https://t.co/raw",
              "entities": {"url": {"urls": [{"url": "https://t.co/raw"}]}}
            }
            """.trimIndent(),
        )

        assertEquals("https://t.co/raw", TwitterUser.fromV1(raw).link)
    }
}


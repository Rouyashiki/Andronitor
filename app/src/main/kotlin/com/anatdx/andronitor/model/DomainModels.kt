package com.anatdx.andronitor.model

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONArray
import org.json.JSONObject

private val domainMoshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
private val twitterUserAdapter = domainMoshi.adapter(TwitterUser::class.java)
private val settingsPayloadAdapter = domainMoshi.adapter(AppSettingsPayload::class.java)

data class Account(
    val id: String,
    val name: String? = null,
    val screenName: String? = null,
    val avatarUrl: String? = null,
    val avatarLocalPath: String? = null,
    val bannerUrl: String? = null,
    val bannerLocalPath: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val link: String? = null,
    val joinTime: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val statusesCount: Int = 0,
    val mediaCount: Int = 0,
    val favouritesCount: Int = 0,
    val listedCount: Int = 0,
    val latestRawJson: String? = null,
    val isVerified: Boolean = false,
    val isProtected: Boolean = false,
)

data class TwitterUser(
    @param:Json(name = "rest_id")
    val restId: String,
    @param:Json(name = "name")
    val name: String? = null,
    @param:Json(name = "screen_name")
    val screenName: String? = null,
    @param:Json(name = "avatar_url")
    val avatarUrl: String? = null,
    @param:Json(name = "avatar_local_path")
    val avatarLocalPath: String? = null,
    @param:Json(name = "banner_url")
    val bannerUrl: String? = null,
    @param:Json(name = "banner_local_path")
    val bannerLocalPath: String? = null,
    @param:Json(name = "bio")
    val bio: String? = null,
    @param:Json(name = "bio_links")
    val bioLinks: List<String> = emptyList(),
    @param:Json(name = "professional_category")
    val professionalCategory: String? = null,
    @param:Json(name = "location")
    val location: String? = null,
    @param:Json(name = "pinned_tweet_id_str")
    val pinnedTweetId: String? = null,
    @param:Json(name = "parody_commentary_fan_label")
    val parodyLabel: String? = null,
    @param:Json(name = "birthdate_year")
    val birthdateYear: String? = null,
    @param:Json(name = "birthdate_month")
    val birthdateMonth: String? = null,
    @param:Json(name = "birthdate_day")
    val birthdateDay: String? = null,
    @param:Json(name = "automated_screen_name")
    val automatedScreenName: String? = null,
    @param:Json(name = "joined_time")
    val joinedTime: String? = null,
    @param:Json(name = "link")
    val link: String? = null,
    @param:Json(name = "status")
    val status: String = "normal",
    @param:Json(name = "kept_ids_status")
    val keptIdsStatus: String = "normal",
    @param:Json(name = "is_verified")
    val isVerified: Boolean = false,
    @param:Json(name = "is_protected")
    val isProtected: Boolean = false,
    @param:Json(name = "followers_count")
    val followersCount: Int = 0,
    @param:Json(name = "following_count")
    val followingCount: Int = 0,
    @param:Json(name = "statuses_count")
    val statusesCount: Int = 0,
    @param:Json(name = "listed_count")
    val listedCount: Int = 0,
    @param:Json(name = "favourites_count")
    val favouritesCount: Int = 0,
    @param:Json(name = "media_count")
    val mediaCount: Int = 0,
    @param:Json(name = "is_following")
    val isFollowing: Boolean = false,
    @param:Json(name = "is_follower")
    val isFollower: Boolean = false,
    @param:Json(name = "can_dm")
    val canDm: Boolean = false,
    @param:Json(name = "can_media_tag")
    val canMediaTag: Boolean = false,
) {
    fun toJsonString(): String = twitterUserAdapter.toJson(this)

    companion object {
        fun fromJsonString(json: String): TwitterUser {
            val parsed = twitterUserAdapter.fromJson(json) ?: error("Invalid Twitter user JSON")
            return parsed.copy(location = unwrapLegacyLocation(parsed.location))
        }

        /**
         * Rows synced before the parser rewrite stored the GraphQL location
         * object verbatim, e.g. {"location":"Taiwan"} — unwrap it on load.
         */
        private fun unwrapLegacyLocation(location: String?): String? {
            if (location == null || !location.startsWith("{")) return location
            return runCatching { JSONObject(location).optNullableString("location") }.getOrElse { location }
        }

        fun fromV1(raw: JSONObject): TwitterUser {
            val id = raw.optString("id_str", raw.optString("id"))
            val entities = raw.optJSONObject("entities")
            val url = entities?.optJSONObject("url")?.optJSONArray("urls")
                ?.optJSONObject(0)?.optNullableString("expanded_url")
            val descriptionUrls = entities?.optJSONObject("description")?.optJSONArray("urls")
            return TwitterUser(
                restId = id,
                name = raw.optNullableString("name"),
                screenName = raw.optNullableString("screen_name"),
                avatarUrl = raw.optNullableString("profile_image_url_https"),
                bannerUrl = raw.optNullableString("profile_banner_url"),
                bio = expandBioUrls(raw.optNullableString("description"), descriptionUrls),
                bioLinks = extractBioLinks(descriptionUrls),
                location = raw.optNullableString("location"),
                link = url ?: raw.optNullableString("url"),
                joinedTime = raw.optNullableString("created_at"),
                isVerified = raw.optBoolean("verified") || raw.optBoolean("is_blue_verified"),
                isProtected = raw.optBoolean("protected"),
                followersCount = raw.optInt("followers_count"),
                followingCount = raw.optInt("friends_count"),
                statusesCount = raw.optInt("statuses_count"),
                listedCount = raw.optInt("listed_count"),
                favouritesCount = raw.optInt("favourites_count"),
                mediaCount = raw.optInt("media_count"),
                isFollowing = raw.optBoolean("following"),
                isFollower = raw.optBoolean("followed_by"),
                canDm = raw.optBoolean("can_dm"),
                canMediaTag = raw.optBoolean("can_media_tag"),
            )
        }

        fun fromGraphQl(raw: JSONObject): TwitterUser {
            val result = unwrapGraphQlUser(raw)
            if (result.optString("__typename") == "UserUnavailable") {
                val message = result.optString("message")
                return TwitterUser(
                    restId = result.optString("rest_id"),
                    name = "User unavailable",
                    screenName = "Unavailable",
                    status = when {
                        message.contains("suspend", true) -> "suspended"
                        message.contains("deactivat", true) -> "deactivated"
                        else -> "unavailable"
                    },
                )
            }
            val legacy = result.optJSONObject("legacy") ?: JSONObject()
            val core = result.optJSONObject("core")
            val relationship = result.optJSONObject("relationship_perspectives")
                ?: result.optJSONObject("relationship")
            val dmPermissions = result.optJSONObject("dm_permissions")
            val mediaPermissions = result.optJSONObject("media_permissions")
            val relationshipCounts = result.optJSONObject("relationship_counts")
            val tweetCounts = result.optJSONObject("tweet_counts")
            val actionCounts = result.optJSONObject("action_counts")
            val profileBio = result.optJSONObject("profile_bio")

            // The current web API moves entities under profile_bio while the legacy
            // shape keeps them under legacy.entities — read whichever is populated.
            val entities = legacy.optJSONObject("entities")?.takeIf { it.length() > 0 }
                ?: profileBio?.optJSONObject("entities")
            val descriptionUrls = entities?.optJSONObject("description")?.optJSONArray("urls")
            val bio = expandBioUrls(
                legacy.optNullableString("description") ?: profileBio?.optNullableString("description"),
                descriptionUrls,
            )
            val link = entities?.optJSONObject("url")?.optJSONArray("urls")
                ?.optJSONObject(0)?.optNullableString("expanded_url")
                ?: legacy.optNullableString("url")
                ?: result.optJSONObject("website")?.optNullableString("url")

            val professionalCategory = result.optJSONObject("professional")?.optJSONArray("category")?.let { categories ->
                (0 until categories.length())
                    .mapNotNull { categories.optJSONObject(it) }
                    .firstOrNull { it.optBoolean("display", true) }
                    ?.optNullableString("name")
            }
            val birthdate = result.optJSONObject("legacy_extended_profile")?.optJSONObject("birthdate")
            val pinnedTweetId = legacy.optJSONArray("pinned_tweet_ids_str")?.optNullableStringAt(0)
                ?: result.optJSONObject("pinned_items")?.optJSONArray("tweet_ids_str")?.optNullableStringAt(0)
            val automatedScreenName = result.optJSONObject("affiliates_highlighted_label")
                ?.optJSONObject("label")?.optJSONObject("longDescription")?.optJSONArray("entities")
                ?.optJSONObject(0)?.optJSONObject("ref")?.optJSONObject("mention_results")
                ?.optJSONObject("result")?.optJSONObject("core")?.optNullableString("screen_name")

            fun count(legacyKey: String, fallback: JSONObject?, fallbackKey: String = legacyKey): Int = when {
                legacy.has(legacyKey) && !legacy.isNull(legacyKey) -> legacy.optInt(legacyKey)
                fallback != null && fallback.has(fallbackKey) -> fallback.optInt(fallbackKey)
                else -> 0
            }

            return TwitterUser(
                restId = result.optString("rest_id", legacy.optString("id_str")),
                name = core?.optNullableString("name") ?: legacy.optNullableString("name"),
                screenName = core?.optNullableString("screen_name") ?: legacy.optNullableString("screen_name"),
                avatarUrl = result.optJSONObject("avatar")?.optNullableString("image_url")
                    ?: legacy.optNullableString("profile_image_url_https"),
                bannerUrl = legacy.optNullableString("profile_banner_url")
                    ?: result.optJSONObject("banner")?.optNullableString("image_url"),
                bio = bio,
                bioLinks = extractBioLinks(descriptionUrls),
                professionalCategory = professionalCategory,
                location = result.optJSONObject("location")?.optNullableString("location")
                    ?: legacy.optNullableString("location"),
                pinnedTweetId = pinnedTweetId,
                parodyLabel = result.optNullableString("parody_commentary_fan_label"),
                birthdateYear = birthdate?.optNullableNumberOrString("year"),
                birthdateMonth = birthdate?.optNullableNumberOrString("month"),
                birthdateDay = birthdate?.optNullableNumberOrString("day"),
                automatedScreenName = automatedScreenName,
                joinedTime = core?.optNullableString("created_at") ?: legacy.optNullableString("created_at"),
                link = link,
                isVerified = legacy.optBoolean("verified") || result.optBoolean("is_blue_verified") ||
                    result.optJSONObject("verification")?.optBoolean("verified") == true,
                isProtected = legacy.optBoolean("protected") ||
                    result.optJSONObject("privacy")?.optBoolean("protected") == true,
                followersCount = count("followers_count", relationshipCounts, "followers"),
                followingCount = count("friends_count", relationshipCounts, "following"),
                statusesCount = count("statuses_count", tweetCounts, "tweets"),
                listedCount = count("listed_count", null),
                favouritesCount = count("favourites_count", actionCounts, "favorites_count"),
                mediaCount = count("media_count", tweetCounts, "media_tweets"),
                isFollowing = relationship?.optBoolean("following") ?: false,
                isFollower = relationship?.optBoolean("followed_by") ?: false,
                canDm = dmPermissions?.optBoolean("can_dm") ?: legacy.optBoolean("can_dm"),
                canMediaTag = mediaPermissions?.optBoolean("can_media_tag") ?: legacy.optBoolean("can_media_tag"),
            )
        }

        /**
         * Unwraps the GraphQL user containers (result / user_results / data.user)
         * until reaching the node that owns `legacy` — reading counters off a
         * wrapper is how valid profiles end up serialized as zero.
         */
        private fun unwrapGraphQlUser(raw: JSONObject): JSONObject {
            var current = raw
            repeat(8) {
                if (current.optJSONObject("legacy") != null) return current
                val next = current.optJSONObject("result")
                    ?: current.optJSONObject("user_results")
                    ?: current.optJSONObject("data")?.optJSONObject("user")
                    ?: return current
                current = next
            }
            return current
        }

        private fun extractBioLinks(urlEntities: JSONArray?): List<String> {
            if (urlEntities == null) return emptyList()
            return (0 until urlEntities.length())
                .mapNotNull { urlEntities.optJSONObject(it)?.optNullableString("expanded_url") }
        }

        /**
         * X sometimes returns a t.co URL in the description that is not
         * byte-for-byte identical to its URL entity. First replace exact
         * entity pairs, then any remaining t.co URLs in entity order.
         */
        private fun expandBioUrls(description: String?, urlEntities: JSONArray?): String? {
            var expanded: String = description ?: return null
            if (urlEntities == null || urlEntities.length() == 0) return description
            val unmatched = mutableListOf<String>()
            for (index in 0 until urlEntities.length()) {
                val entity = urlEntities.optJSONObject(index) ?: continue
                val shortUrl = entity.optNullableString("url")
                val expandedUrl = entity.optNullableString("expanded_url")
                    ?: entity.optNullableString("display_url")
                    ?: continue
                if (shortUrl != null && expanded.contains(shortUrl)) {
                    expanded = expanded.replace(shortUrl, expandedUrl)
                } else {
                    unmatched += expandedUrl
                }
            }
            expanded = Regex("https?://t\\.co/[A-Za-z0-9]+").replace(expanded) { match ->
                if (unmatched.isEmpty()) match.value else unmatched.removeAt(0)
            }
            return expanded.trim()
        }
    }
}

data class Relation(
    val ownerId: String,
    val user: TwitterUser,
    val isFollower: Boolean,
    val isFollowing: Boolean,
    val runId: String? = null,
)

enum class ChangeType(val key: String, val title: String) {
    PROFILE_UPDATE("profile_update", "Profile updates"),
    NORMAL_UNFOLLOWED("normal_unfollowed", "Unfollowed"),
    MUTUAL_UNFOLLOWED("mutual_unfollowed", "Mutual unfollowed"),
    ONEWAY_UNFOLLOWED("oneway_unfollowed", "One-way unfollowed"),
    TEMPORARILY_RESTRICTED("temporarily_restricted", "Temporarily restricted"),
    SUSPENDED("suspended", "Suspended"),
    DEACTIVATED("deactivated", "Deactivated"),
    BE_FOLLOWED_BACK("be_followed_back", "Followed back"),
    NEW_FOLLOWERS_FOLLOWING("new_followers_following", "New followers/following"),
    RECOVERED("recovered", "Recovered"),
    OTHER_REASONS("other_reasons", "Other reasons");

    companion object {
        fun fromKey(key: String): ChangeType = entries.firstOrNull { it.key == key } ?: OTHER_REASONS
    }
}

data class ChangeReport(
    val id: Long,
    val ownerId: String,
    val userId: String,
    val changeType: String,
    val timestamp: Long,
    val userSnapshotJson: String?,
)

data class DashboardStats(
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val counts: Map<String, Int> = emptyMap(),
    val lastSync: Long? = null,
)

enum class SyncFetchMode { FAST, EXPERT, FASTEST }
enum class MediaSaveScope { ALL, FOLLOWING_ONLY, MUTUAL_ONLY }

data class AppSettings(
    val apiRequestMode: String = "dio",
    val remoteFastApiUrl: String = "",
    val fastApiApiKey: String = "",
    val saveAvatarHistory: Boolean = true,
    val saveBannerHistory: Boolean = false,
    val mediaSaveScope: MediaSaveScope = MediaSaveScope.ALL,
    val syncFetchMode: SyncFetchMode = SyncFetchMode.FAST,
    val avatarQualityHigh: Boolean = false,
    val historyLimit: Int = 5,
    val darkTheme: Boolean = false,
    val language: String = "system",
    val customQueryIds: Map<String, String> = emptyMap(),
) {
    fun toJsonString(): String = settingsPayloadAdapter.toJson(toPayload())

    companion object {
        fun fromJsonString(json: String): AppSettings = runCatching {
            settingsPayloadAdapter.fromJson(json)?.toSettings() ?: AppSettings()
        }.getOrDefault(AppSettings())
    }

    private fun toPayload() = AppSettingsPayload(
        apiRequestMode = apiRequestMode,
        remoteFastApiUrl = remoteFastApiUrl,
        fastApiApiKey = fastApiApiKey,
        saveAvatarHistory = saveAvatarHistory,
        saveBannerHistory = saveBannerHistory,
        mediaSaveScope = mediaSaveScope.name,
        syncFetchMode = syncFetchMode.name,
        avatarQualityHigh = avatarQualityHigh,
        historyLimit = historyLimit,
        darkTheme = darkTheme,
        language = language,
        customQueryIds = customQueryIds,
    )
}

private data class AppSettingsPayload(
    val apiRequestMode: String = "dio",
    val remoteFastApiUrl: String = "",
    val fastApiApiKey: String = "",
    val saveAvatarHistory: Boolean = true,
    val saveBannerHistory: Boolean = false,
    val mediaSaveScope: String = "ALL",
    val syncFetchMode: String = "FAST",
    val avatarQualityHigh: Boolean = false,
    val historyLimit: Int = 5,
    val darkTheme: Boolean = false,
    val language: String = "system",
    val customQueryIds: Map<String, String> = emptyMap(),
) {
    fun toSettings(): AppSettings = AppSettings(
        apiRequestMode = apiRequestMode,
        remoteFastApiUrl = remoteFastApiUrl,
        fastApiApiKey = fastApiApiKey,
        saveAvatarHistory = saveAvatarHistory,
        saveBannerHistory = saveBannerHistory,
        mediaSaveScope = runCatching { MediaSaveScope.valueOf(mediaSaveScope) }.getOrDefault(MediaSaveScope.ALL),
        syncFetchMode = runCatching { SyncFetchMode.valueOf(syncFetchMode) }.getOrDefault(SyncFetchMode.FAST),
        avatarQualityHigh = avatarQualityHigh,
        historyLimit = historyLimit.coerceAtLeast(1),
        darkTheme = darkTheme,
        language = language,
        customQueryIds = customQueryIds,
    )
}

data class SyncProgress(
    val stage: String,
    val completed: Int = 0,
    val total: Int = 0,
    val message: String = "",
) {
    val fraction: Float?
        get() = if (total > 0) (completed.toFloat() / total).coerceIn(0f, 1f) else null
}

private fun JSONObject.optNullableString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

/** Birthdate parts arrive as numbers; normalize to a display string. */
private fun JSONObject.optNullableNumberOrString(key: String): String? =
    if (!has(key) || isNull(key)) null else opt(key)?.toString()?.takeIf { it.isNotEmpty() }

private fun JSONArray.optNullableStringAt(index: Int): String? =
    if (index >= length() || isNull(index)) null else optString(index).takeIf { it.isNotEmpty() }

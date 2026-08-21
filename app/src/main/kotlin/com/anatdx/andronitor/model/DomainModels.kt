package com.anatdx.andronitor.model

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
    @param:Json(name = "professional_category")
    val professionalCategory: String? = null,
    @param:Json(name = "location")
    val location: String? = null,
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
        fun fromJsonString(json: String): TwitterUser = twitterUserAdapter.fromJson(json)
            ?: error("Invalid Twitter user JSON")

        fun fromV1(raw: JSONObject): TwitterUser {
            val id = raw.optString("id_str", raw.optString("id"))
            val entities = raw.optJSONObject("entities")
            val url = entities?.optJSONObject("url")?.optJSONArray("urls")
                ?.optJSONObject(0)?.optString("expanded_url")
            return TwitterUser(
                restId = id,
                name = raw.optNullableString("name"),
                screenName = raw.optNullableString("screen_name"),
                avatarUrl = raw.optNullableString("profile_image_url_https"),
                bannerUrl = raw.optNullableString("profile_banner_url"),
                bio = raw.optNullableString("description"),
                location = raw.optNullableString("location"),
                link = url,
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
            val result = raw.optJSONObject("result") ?: raw
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
            val legacy = result.optJSONObject("legacy") ?: result
            val core = result.optJSONObject("core")
            val relationship = result.optJSONObject("relationship_perspectives")
                ?: result.optJSONObject("relationship")
            val userId = result.optString("rest_id", legacy.optString("id_str"))
            return TwitterUser(
                restId = userId,
                name = core?.optNullableString("name") ?: legacy.optNullableString("name"),
                screenName = core?.optNullableString("screen_name") ?: legacy.optNullableString("screen_name"),
                avatarUrl = legacy.optNullableString("profile_image_url_https"),
                bannerUrl = legacy.optNullableString("profile_banner_url"),
                bio = legacy.optNullableString("description"),
                location = legacy.optNullableString("location"),
                joinedTime = legacy.optNullableString("created_at"),
                isVerified = legacy.optBoolean("verified") || result.optBoolean("is_blue_verified"),
                isProtected = legacy.optBoolean("protected"),
                followersCount = result.optJSONObject("relationship_counts")?.optInt("followers")
                    ?: legacy.optInt("followers_count"),
                followingCount = result.optJSONObject("relationship_counts")?.optInt("following")
                    ?: legacy.optInt("friends_count"),
                statusesCount = legacy.optInt("statuses_count"),
                listedCount = legacy.optInt("listed_count"),
                favouritesCount = legacy.optInt("favourites_count"),
                mediaCount = legacy.optInt("media_count"),
                isFollowing = relationship?.optBoolean("following") ?: false,
                isFollower = relationship?.optBoolean("followed_by") ?: false,
            )
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

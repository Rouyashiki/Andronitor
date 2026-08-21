package com.anatdx.andronitor.network

import android.util.Base64
import com.anatdx.andronitor.model.AppSettings
import com.anatdx.andronitor.model.TwitterUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

data class UserPage(
    val users: List<TwitterUser>,
    val nextCursor: String?,
)

class XApiClient {
    companion object {
        private const val BEARER = "Bearer AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xnZz4puTs%3D1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpTnA"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/138.0 Mobile Safari/537.36"
        private val DEFAULT_QUERY_IDS = mapOf(
            "UserByRestId" to "Bbaot8ySMtJD7K2t01gW7A",
            "UsersByRestIds" to "xavgLWWbFH8wm_8MQN8plQ",
            "UsersByScreenNames" to "ujL_oXbgVlDHQzWSTgzvnA",
            "Followers" to "oQWxG6XdR5SPvMBsPiKUPQ",
            "Following" to "i2GOldCH2D3OUEhAdimLrA",
        )
    }

    suspend fun getProfile(userId: String, cookie: String, settings: AppSettings): TwitterUser {
        val csrf = csrf(cookie)
        val queryId = settings.customQueryIds["UserByRestId"] ?: DEFAULT_QUERY_IDS.getValue("UserByRestId")
        val variables = JSONObject().apply {
            put("userId", userId)
            put("withGrokTranslatedBio", true)
            put("withSafetyModeUserFields", true)
        }
        val features = JSONObject().apply {
            put("hidden_profile_subscriptions_enabled", true)
            put("profile_label_improvements_pcf_label_in_post_enabled", true)
            put("responsive_web_profile_redirect_enabled", true)
            put("rweb_tipjar_consumption_enabled", true)
            put("verified_phone_label_enabled", true)
            put("subscriptions_verification_info_is_identity_verified_enabled", true)
            put("subscriptions_verification_info_verified_since_enabled", true)
            put("highlights_tweets_tab_ui_enabled", true)
            put("responsive_web_twitter_article_notes_tab_enabled", true)
            put("responsive_web_graphql_skip_user_profile_image_extensions_enabled", true)
            put("responsive_web_graphql_timeline_navigation_enabled", true)
            put("payments_enabled", true)
            put("responsive_web_graphql_exclude_directive_enabled", true)
        }
        val body = get(
            "https://api.x.com/graphql/$queryId/UserByRestId",
            mapOf("variables" to variables.toString(), "features" to features.toString()),
            headers(cookie, csrf), settings,
        ) as? JSONObject ?: throw IllegalStateException("X returned a non-JSON profile response")
        val user = body.optJSONObject("data")?.optJSONObject("user")?.let(TwitterUser::fromGraphQl)
            ?: throw IllegalStateException("X returned no profile for $userId")
        if (user.restId.isNotEmpty() && user.restId != userId) {
            throw IllegalStateException("Cookie twid does not match returned profile")
        }
        return user.copy(restId = userId)
    }

    suspend fun getFollowers(userId: String, cookie: String, cursor: String?, settings: AppSettings): UserPage =
        getV1List("https://api.x.com/1.1/followers/list.json", userId, cookie, cursor, settings)

    suspend fun getFollowingV1(userId: String, cookie: String, cursor: String?, settings: AppSettings): UserPage =
        getV1List("https://api.x.com/1.1/friends/list.json", userId, cookie, cursor, settings)

    suspend fun getFollowingGraphQl(
        userId: String,
        cookie: String,
        cursor: String,
        settings: AppSettings,
    ): UserPage {
        val csrf = csrf(cookie)
        val queryId = settings.customQueryIds["Following"] ?: DEFAULT_QUERY_IDS.getValue("Following")
        val variables = JSONObject().apply {
            put("userId", userId)
            put("count", 50)
            put("includePromotedContent", false)
            put("withGrokTranslatedBio", false)
            put("cursor", cursor)
        }
        val features = JSONObject().apply {
            put("hidden_profile_subscriptions_enabled", true)
            put("responsive_web_graphql_exclude_directive_enabled", true)
            put("verified_phone_label_enabled", true)
            put("highlights_tweets_tab_ui_enabled", true)
            put("creator_subscriptions_tweet_preview_api_enabled", true)
            put("responsive_web_graphql_skip_user_profile_image_extensions_enabled", true)
            put("responsive_web_graphql_timeline_navigation_enabled", true)
            put("rweb_tipjar_consumption_enabled", true)
            put("subscriptions_feature_can_gift_premium", true)
            put("payments_enabled", true)
            put("responsive_web_twitter_article_notes_tab_enabled", true)
            put("profile_label_improvements_pcf_label_in_post_enabled", true)
            put("responsive_web_profile_redirect_enabled", true)
            put("post_ctas_fetch_enabled", false)
        }
        val body = get(
            "https://api.x.com/graphql/$queryId/Following",
            mapOf("variables" to variables.toString(), "features" to features.toString()),
            headers(cookie, csrf), settings,
        ) as? JSONObject ?: return UserPage(emptyList(), null)
        val direct = body.optJSONObject("data")?.optJSONArray("users")
        if (direct != null) return UserPage(parseGraphQlUsers(direct), null)
        val instructions = body.optJSONObject("data")?.optJSONObject("user")?.optJSONObject("result")
            ?.optJSONObject("timeline")?.optJSONObject("timeline")?.optJSONArray("instructions")
            ?: return UserPage(emptyList(), null)
        var nextCursor: String? = null
        val users = mutableListOf<TwitterUser>()
        for (index in 0 until instructions.length()) {
            val instruction = instructions.optJSONObject(index) ?: continue
            val entries = instruction.optJSONArray("entries") ?: continue
            for (entryIndex in 0 until entries.length()) {
                val entry = entries.optJSONObject(entryIndex) ?: continue
                val content = entry.optJSONObject("content") ?: continue
                if (content.optString("entryType") == "TimelineTimelineCursor" && content.optString("cursorType") == "Bottom") {
                    nextCursor = content.optString("value").takeIf { it.isNotEmpty() }
                }
                if (content.optString("entryType") == "TimelineTimelineItem") {
                    val item = content.optJSONObject("itemContent")
                    val result = item?.optJSONObject("user_results")
                    if (result != null) users += TwitterUser.fromGraphQl(result)
                }
            }
        }
        return UserPage(users.filter { it.restId.isNotEmpty() }, nextCursor)
    }

    suspend fun refreshQueryIds(): Map<String, String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/fa0311/TwitterInternalAPIDocument/refs/heads/develop/docs/json/GraphQL.json")
            .get()
            .build()
        HttpClientProvider.client(15_000, 30_000).newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiException(response.code, raw.take(500))
            val array = JSONArray(raw)
            val wanted = DEFAULT_QUERY_IDS.keys
            buildMap {
                for (index in 0 until array.length()) {
                    val exports = array.optJSONObject(index)?.optJSONObject("exports") ?: continue
                    val name = exports.optString("operationName")
                    if (name in wanted) put(name, exports.optString("queryId"))
                }
            }
        }
    }

    private suspend fun getV1List(
        endpoint: String,
        userId: String,
        cookie: String,
        cursor: String?,
        settings: AppSettings,
    ): UserPage {
        val csrf = csrf(cookie)
        val params = linkedMapOf(
            "user_id" to userId,
            "count" to "200",
            "include_profile_interstitial_type" to "1",
            "include_blocking" to "0",
            "include_blocked_by" to "0",
            "include_followed_by" to "1",
            "include_want_retweets" to "0",
            "include_mute_edge" to "0",
            "include_can_dm" to "1",
            "include_can_media_tag" to "1",
            "skip_status" to "1",
        )
        if (!cursor.isNullOrEmpty() && cursor != "0") params["cursor"] = cursor
        val body = get(endpoint, params, headers(cookie, csrf), settings)
        if (body !is JSONObject) return UserPage(emptyList(), null)
        val users = body.optJSONArray("users") ?: JSONArray()
        return UserPage(
            buildList { for (index in 0 until users.length()) add(TwitterUser.fromV1(users.getJSONObject(index))) },
            body.optString("next_cursor_str", body.optLong("next_cursor").toString()).takeIf { it != "0" && it.isNotEmpty() },
        )
    }

    private fun parseGraphQlUsers(array: JSONArray): List<TwitterUser> = buildList {
        for (index in 0 until array.length()) {
            val wrapper = array.optJSONObject(index) ?: continue
            val result = wrapper.optJSONObject("result") ?: continue
            add(TwitterUser.fromGraphQl(result))
        }
    }

    private fun csrf(cookie: String): String = cookie.split(';')
        .firstOrNull { it.trim().startsWith("ct0=") }
        ?.substringAfter('=')?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: throw IllegalArgumentException("Cookie does not contain ct0")

    private fun headers(cookie: String, csrf: String): Map<String, String> = mapOf(
        "authorization" to BEARER,
        "x-csrf-token" to csrf,
        "Cookie" to cookie,
        "x-twitter-active-user" to "yes",
        "x-twitter-auth-type" to "OAuth2Session",
        "x-twitter-client-language" to "en",
        "User-Agent" to USER_AGENT,
        "Referer" to "https://x.com/",
    )

    private suspend fun get(
        endpoint: String,
        params: Map<String, String>,
        headers: Map<String, String>,
        settings: AppSettings,
    ): Any = withContext(Dispatchers.IO) {
        val targetUrl = endpoint.toHttpUrl().newBuilder().apply {
            params.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build().toString()
        val requestUrl: String
        val requestHeaders: Map<String, String>
        if (settings.apiRequestMode == "curl_cffi" && settings.remoteFastApiUrl.isNotBlank()) {
            requestUrl = settings.remoteFastApiUrl.toHttpUrl().newBuilder()
                .addQueryParameter("target_url", targetUrl)
                .addQueryParameter("req_headers", JSONObject(headers).toString())
                .addQueryParameter("apikey", settings.fastApiApiKey)
                .build()
                .toString()
            requestHeaders = emptyMap()
        } else {
            requestUrl = targetUrl
            requestHeaders = headers
        }
        val requestBuilder = Request.Builder().url(requestUrl)
        requestHeaders.forEach { (key, value) -> requestBuilder.header(key, value) }
        val request = requestBuilder.get().build()
        HttpClientProvider.client(20_000, 60_000).newCall(request).execute().use { response ->
            val status = response.code
            val text = response.body?.string().orEmpty()
            if (status == 429) throw RateLimitException(endpoint)
            if (!response.isSuccessful) throw ApiException(status, text.take(500))
            val parsed = runCatching { JSONObject(text) }.getOrElse {
                runCatching { JSONArray(text) }.getOrElse { text }
            }
            if (settings.apiRequestMode == "curl_cffi" && parsed is JSONObject && parsed.optString("proxy_status") == "success") {
                val bytes = Base64.decode(parsed.optString("raw_response_base64"), Base64.DEFAULT)
                val raw = String(bytes, StandardCharsets.UTF_8)
                return@withContext runCatching { JSONObject(raw) }.getOrElse {
                    runCatching { JSONArray(raw) }.getOrElse { raw }
                }
            }
            parsed
        }
    }

}

class ApiException(val statusCode: Int, message: String) : IllegalStateException("X API $statusCode: $message")
class RateLimitException(endpoint: String) : IllegalStateException("X API rate limit reached: $endpoint")

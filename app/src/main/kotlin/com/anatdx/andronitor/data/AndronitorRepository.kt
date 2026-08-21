package com.anatdx.andronitor.data

import android.content.Context
import android.net.Uri
import com.anatdx.andronitor.model.Account
import com.anatdx.andronitor.model.AppSettings
import com.anatdx.andronitor.model.DashboardStats
import com.anatdx.andronitor.model.SearchFilters
import com.anatdx.andronitor.model.SyncFetchMode
import com.anatdx.andronitor.model.SyncProgress
import com.anatdx.andronitor.model.TwitterUser
import com.anatdx.andronitor.network.XApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class AndronitorRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = AndronitorDatabase(appContext)
    private val cookies = SecureCookieStore(appContext)
    private val settingsStore = SettingsStore(appContext)
    private val api = XApiClient()
    private val media = MediaHistoryService(appContext, database)
    private val backup = BackupService(appContext, database, settingsStore, cookies)
    private val searchHistory = SearchHistoryStore(appContext)

    fun loadSettings(): AppSettings = settingsStore.load()

    fun saveSettings(settings: AppSettings) = settingsStore.save(settings)

    suspend fun loadAccounts(): List<Account> = withContext(Dispatchers.IO) {
        database.getAccounts().map { account -> account.copy() }
    }

    fun activeAccountId(): String? = cookies.activeAccountId()

    fun setActiveAccount(id: String?) = cookies.setActiveAccountId(id)

    suspend fun addAccount(cookie: String, settings: AppSettings): Account = withContext(Dispatchers.IO) {
        val id = parseIdFromCookie(cookie) ?: throw IllegalArgumentException("Cookie must contain twid")
        val profile = api.getProfile(id, cookie, settings)
        if (profile.restId.isNotEmpty() && profile.restId != id) throw IllegalStateException("Cookie account mismatch")
        val account = profile.toAccount(id, cookie)
        database.upsertAccount(account)
        cookies.saveCookie(id, cookie)
        cookies.setActiveAccountId(id)
        account
    }

    suspend fun updateAccountCookie(accountId: String, cookie: String) = withContext(Dispatchers.IO) {
        val id = parseIdFromCookie(cookie) ?: throw IllegalArgumentException("Cookie must contain twid")
        require(id == accountId) { "Cookie twid does not match this account" }
        cookies.saveCookie(accountId, cookie)
    }

    suspend fun removeAccount(accountId: String) = withContext(Dispatchers.IO) {
        cookies.deleteCookie(accountId)
        database.deleteAccount(accountId)
        if (cookies.activeAccountId() == accountId) cookies.setActiveAccountId(database.getAccounts().firstOrNull()?.id)
    }

    suspend fun refreshAccount(account: Account, settings: AppSettings): Account = withContext(Dispatchers.IO) {
        val cookie = cookies.getCookie(account.id) ?: throw IllegalStateException("Account cookie is missing")
        val profile = api.getProfile(account.id, cookie, settings)
        profile.toAccount(account.id, cookie, account)
            .also(database::upsertAccount)
    }

    suspend fun sync(
        account: Account,
        settings: AppSettings,
        mode: SyncFetchMode = settings.syncFetchMode,
        onProgress: (SyncProgress) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        val cookie = cookies.getCookie(account.id) ?: throw IllegalStateException("Add the account cookie before syncing")
        val followers = linkedMapOf<String, TwitterUser>()
        val following = linkedMapOf<String, TwitterUser>()
        val followerIds = linkedSetOf<String>()
        val followingIds = linkedSetOf<String>()
        onProgress(SyncProgress("Refreshing profile", message = account.screenName ?: account.id))
        try {
            val refreshed = api.getProfile(account.id, cookie, settings)
            database.upsertAccount(refreshed.toAccount(account.id, cookie, account))
        } catch (_: Exception) {
            // A profile refresh should not discard a sync when the profile endpoint is transiently unavailable.
        }

        onProgress(SyncProgress("Fetching followers", message = "Starting"))
        var cursor: String? = null
        var page = 0
        do {
            val result = api.getFollowers(account.id, cookie, cursor, settings)
            result.users.forEach { user ->
                if (user.restId.isNotEmpty()) {
                    followers[user.restId] = user
                    followerIds += user.restId
                }
            }
            cursor = result.nextCursor
            page++
            onProgress(SyncProgress("Fetching followers", page, 0, "${followers.size} users"))
        } while (!cursor.isNullOrEmpty() && cursor != "0")

        onProgress(SyncProgress("Fetching following", message = "Starting"))
        cursor = if (mode == SyncFetchMode.FAST || mode == SyncFetchMode.FASTEST) "-1" else null
        page = 0
        do {
            val result = if (mode == SyncFetchMode.FAST || mode == SyncFetchMode.FASTEST) {
                try {
                    api.getFollowingGraphQl(account.id, cookie, cursor ?: "-1", settings)
                } catch (_: Exception) {
                    api.getFollowingV1(account.id, cookie, cursor, settings)
                }
            } else {
                api.getFollowingV1(account.id, cookie, cursor, settings)
            }
            result.users.forEach { user ->
                if (user.restId.isNotEmpty()) {
                    following[user.restId] = user
                    followingIds += user.restId
                }
            }
            cursor = result.nextCursor
            page++
            onProgress(SyncProgress("Fetching following", page, 0, "${following.size} users"))
        } while (!cursor.isNullOrEmpty() && cursor != "0" && !cursor.startsWith("0|"))

        val users = linkedMapOf<String, TwitterUser>()
        users.putAll(followers)
        users.putAll(following)
        onProgress(SyncProgress("Analyzing changes", 0, users.size, "Comparing local history"))
        val runId = UUID.randomUUID().toString()
        database.persistSync(account.id, users, followerIds, followingIds, runId)
        onProgress(SyncProgress("Downloading media", 0, users.size, "Checking saved media"))
        media.process(account.id, users.values, followerIds, followingIds, settings) { completed, total, name ->
            onProgress(SyncProgress("Downloading media", completed, total, name))
        }
        onProgress(SyncProgress("Completed", users.size, users.size, "Sync finished"))
    }

    suspend fun dashboard(accountId: String): DashboardStats = withContext(Dispatchers.IO) {
        database.getDashboardStats(accountId)
    }

    suspend fun usersForCategory(accountId: String, category: String): List<TwitterUser> = withContext(Dispatchers.IO) {
        database.getUsersForCategory(accountId, category)
    }

    suspend fun reportsForUser(accountId: String, userId: String) = withContext(Dispatchers.IO) {
        database.getReports(accountId, userId)
    }

    suspend fun search(accountId: String, query: String, filters: SearchFilters): List<TwitterUser> =
        withContext(Dispatchers.IO) {
            database.searchUsers(accountId, query, filters)
        }

    fun searchHistory(): List<String> = searchHistory.load()

    fun rememberSearch(term: String): List<String> = searchHistory.add(term)

    fun forgetSearch(term: String): List<String> = searchHistory.remove(term)

    fun clearSearchHistory(): List<String> = searchHistory.clear()

    suspend fun refreshQueryIds(settings: AppSettings): AppSettings = withContext(Dispatchers.IO) {
        val ids = api.refreshQueryIds()
        settings.copy(customQueryIds = settings.customQueryIds + ids).also(settingsStore::save)
    }

    suspend fun writeBackup(destination: Uri) = backup.writeFullBackup(destination)

    suspend fun restoreBackup(source: Uri) {
        backup.restoreFullBackup(source)
    }

    /** Public: the X user id encoded in a cookie's twid, or null if absent. */
    fun accountIdForCookie(cookie: String): String? = parseIdFromCookie(cookie)

    private fun parseIdFromCookie(cookie: String): String? {
        val raw = cookie.split(';').firstOrNull { it.trim().startsWith("twid=") }
            ?.substringAfter('=')?.trim() ?: return null
        val decoded = runCatching { java.net.URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
        return decoded.substringAfter("u=", decoded.substringAfter("u_", "")).takeIf { it.isNotBlank() }
    }

    private fun TwitterUser.toAccount(id: String, cookie: String, previous: Account? = null): Account = Account(
        id = id,
        name = name,
        screenName = screenName,
        avatarUrl = avatarUrl,
        avatarLocalPath = avatarLocalPath ?: previous?.avatarLocalPath,
        bannerUrl = bannerUrl,
        bannerLocalPath = bannerLocalPath ?: previous?.bannerLocalPath,
        bio = bio,
        location = location,
        link = link,
        joinTime = joinedTime,
        followersCount = followersCount,
        followingCount = followingCount,
        statusesCount = statusesCount,
        mediaCount = mediaCount,
        favouritesCount = favouritesCount,
        listedCount = listedCount,
        latestRawJson = toJsonString(),
        isVerified = isVerified,
        isProtected = isProtected,
    )
}

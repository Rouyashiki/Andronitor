package com.anatdx.andronitor.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.anatdx.andronitor.model.Account
import com.anatdx.andronitor.model.ChangeReport
import com.anatdx.andronitor.model.ChangeType
import com.anatdx.andronitor.model.DashboardStats
import com.anatdx.andronitor.model.Relation
import com.anatdx.andronitor.model.TwitterUser
import org.json.JSONObject

class AndronitorDatabase(context: Context) : AutoCloseable {
    companion object {
        private const val DATABASE_NAME = "andronitor_data.db"
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE media_history_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        media_type TEXT NOT NULL,
                        local_file_path TEXT NOT NULL,
                        remote_url TEXT NOT NULL,
                        is_high_quality INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO media_history_new(id, media_type, local_file_path, remote_url, is_high_quality)
                    SELECT id, media_type, local_file_path, remote_url, is_high_quality FROM media_history
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE media_history")
                db.execSQL("ALTER TABLE media_history_new RENAME TO media_history")
                db.execSQL("CREATE UNIQUE INDEX index_media_history_remote_url ON media_history(remote_url)")
            }
        }
    }

    private val appContext = context.applicationContext
    private var room = openRoom()
    private var dao = room.dao()

    private fun openRoom() = Room.databaseBuilder(
        appContext,
        AndronitorRoomDatabase::class.java,
        DATABASE_NAME,
    ).addMigrations(MIGRATION_1_2).build()

    fun getAccounts(): List<Account> = dao.getAccounts().map(LoggedAccountEntity::toAccount)

    fun getAccount(id: String): Account? = dao.getAccount(id)?.toAccount()

    fun upsertAccount(account: Account) = dao.upsertAccount(account.toEntity())

    fun deleteAccount(id: String) {
        room.runInTransaction {
            dao.deleteReports(id)
            dao.deleteHistory(id)
            dao.deleteRelations(id)
            dao.deleteProfileHistory(id)
            dao.deleteSyncLogs(id)
            dao.deleteAccount(id)
        }
    }

    fun getRelations(ownerId: String): Map<String, Relation> = dao.getRelations(ownerId)
        .associate { relation -> relation.userId to relation.toRelation() }

    fun persistSync(
        ownerId: String,
        users: Map<String, TwitterUser>,
        followerIds: Set<String>,
        followingIds: Set<String>,
        runId: String,
    ): List<ChangeReport> {
        val old = getRelations(ownerId)
        val now = System.currentTimeMillis()
        val reports = mutableListOf<ChangeReport>()
        room.runInTransaction {
            val newIds = users.keys
            val oldIds = old.keys
            for (id in oldIds - newIds) {
                val previous = old.getValue(id)
                val type = when {
                    previous.isFollower && previous.isFollowing -> ChangeType.MUTUAL_UNFOLLOWED
                    previous.isFollower || previous.isFollowing -> ChangeType.NORMAL_UNFOLLOWED
                    else -> null
                }
                if (type != null) reports += insertReport(
                    ownerId, id, type.key, now, previous.user.toJsonString(),
                )
            }
            for ((id, user) in users) {
                val current = user.copy(
                    isFollower = id in followerIds,
                    isFollowing = id in followingIds,
                )
                val previous = old[id]
                if (previous == null) {
                    reports += insertReport(
                        ownerId, id, ChangeType.NEW_FOLLOWERS_FOLLOWING.key, now, current.toJsonString(),
                    )
                } else {
                    classifyRelationshipChange(previous, current, ownerId, id, now, reports)
                    if (profileChanged(previous.user, current)) {
                        reports += insertReport(ownerId, id, ChangeType.PROFILE_UPDATE.key, now, current.toJsonString())
                    }
                }
                dao.upsertRelation(current.toEntity(ownerId, runId))
                dao.insertHistory(
                    FollowUserHistoryEntity(
                        ownerId = ownerId,
                        userId = id,
                        reverseDiffJson = "{}",
                        runId = runId,
                        timestamp = now,
                    ),
                )
            }
            for (id in oldIds - newIds) dao.deleteRelation(ownerId, id)
            dao.insertSyncLog(SyncLogEntity(runId = runId, timestamp = now, status = 1, ownerId = ownerId))
        }
        return reports
    }

    fun getDashboardStats(ownerId: String): DashboardStats {
        val account = getAccount(ownerId)
        val counts = dao.getReportCounts(ownerId).associate { it.changeType to it.count }
        return DashboardStats(account?.followersCount ?: 0, account?.followingCount ?: 0, counts, dao.getLastSync(ownerId))
    }

    fun getUsersForCategory(ownerId: String, category: String): List<TwitterUser> {
        if (category == "followers") return dao.getFollowers(ownerId).map(FollowUserEntity::toUser)
        if (category == "following") return dao.getFollowing(ownerId).map(FollowUserEntity::toUser)
        return dao.getReportsForCategory(ownerId, category).mapNotNull { report ->
            report.userSnapshotJson.parseUser() ?: dao.getRelation(ownerId, report.userId)?.toUser()
        }
    }

    fun getReports(ownerId: String, userId: String): List<ChangeReport> =
        dao.getReports(ownerId, userId).map(ChangeReportEntity::toChangeReport)

    fun getMediaRecord(remoteUrl: String): MediaRecord? = dao.getMediaRecord(remoteUrl)?.toMediaRecord()

    fun upsertMediaRecord(record: MediaRecord) = dao.upsertMediaRecord(record.toEntity())

    fun checkpointForBackup() {
        room.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
    }

    fun replaceFromBackup(source: java.io.File) {
        room.close()
        val destination = appContext.getDatabasePath(DATABASE_NAME)
        destination.parentFile?.mkdirs()
        java.io.File(destination.path + "-wal").delete()
        java.io.File(destination.path + "-shm").delete()
        source.copyTo(destination, overwrite = true)
        room = openRoom()
        dao = room.dao()
    }

    override fun close() = room.close()

    private fun classifyRelationshipChange(
        previous: Relation,
        current: TwitterUser,
        ownerId: String,
        userId: String,
        now: Long,
        reports: MutableList<ChangeReport>,
    ) {
        val wasMutual = previous.isFollower && previous.isFollowing
        val isMutual = current.isFollower && current.isFollowing
        val becameMutual = isMutual && (previous.isFollower != previous.isFollowing)
        val becameOneWay = wasMutual && (current.isFollower != current.isFollowing)
        val lostFollowing = previous.isFollowing && !current.isFollowing
        val lostFollower = previous.isFollower && !current.isFollower
        when {
            current.status == "temporarily_restricted" -> reports += insertReport(
                ownerId, userId, ChangeType.TEMPORARILY_RESTRICTED.key, now, current.toJsonString(),
            )
            previous.user.status in setOf("suspended", "deactivated") && current.status == "normal" -> reports += insertReport(
                ownerId, userId, ChangeType.RECOVERED.key, now, current.toJsonString(),
            )
            becameMutual -> reports += insertReport(
                ownerId, userId, ChangeType.BE_FOLLOWED_BACK.key, now, current.toJsonString(),
            )
            becameOneWay -> reports += insertReport(
                ownerId, userId, ChangeType.ONEWAY_UNFOLLOWED.key, now, current.toJsonString(),
            )
            lostFollowing || lostFollower -> reports += insertReport(
                ownerId, userId, ChangeType.NORMAL_UNFOLLOWED.key, now, current.toJsonString(),
            )
        }
    }

    private fun insertReport(
        ownerId: String,
        userId: String,
        type: String,
        timestamp: Long,
        snapshot: String?,
    ): ChangeReport {
        val id = dao.insertReport(ChangeReportEntity(0, ownerId, userId, type, timestamp, snapshot))
        return ChangeReport(id, ownerId, userId, type, timestamp, snapshot)
    }

    private fun profileChanged(old: TwitterUser, new: TwitterUser): Boolean =
        old.name != new.name || old.screenName != new.screenName || old.bio != new.bio ||
            old.avatarUrl != new.avatarUrl || old.bannerUrl != new.bannerUrl || old.location != new.location ||
            old.isVerified != new.isVerified || old.isProtected != new.isProtected
}

private fun Account.toEntity() = LoggedAccountEntity(
    id = id,
    name = name,
    screenName = screenName,
    bio = bio,
    location = location,
    link = link,
    joinTime = joinTime,
    isVerified = isVerified.asInt(),
    isProtected = isProtected.asInt(),
    followersCount = followersCount,
    followingCount = followingCount,
    statusesCount = statusesCount,
    mediaCount = mediaCount,
    favouritesCount = favouritesCount,
    listedCount = listedCount,
    latestRawJson = latestRawJson,
    avatarUrl = avatarUrl,
    bannerUrl = bannerUrl,
    avatarLocalPath = avatarLocalPath,
    bannerLocalPath = bannerLocalPath,
)

private fun LoggedAccountEntity.toAccount() = Account(
    id = id,
    name = name,
    screenName = screenName,
    avatarUrl = avatarUrl,
    avatarLocalPath = avatarLocalPath,
    bannerUrl = bannerUrl,
    bannerLocalPath = bannerLocalPath,
    bio = bio,
    location = location,
    link = link,
    joinTime = joinTime,
    followersCount = followersCount,
    followingCount = followingCount,
    statusesCount = statusesCount,
    mediaCount = mediaCount,
    favouritesCount = favouritesCount,
    listedCount = listedCount,
    latestRawJson = latestRawJson,
    isVerified = isVerified != 0,
    isProtected = isProtected != 0,
)

private fun TwitterUser.toEntity(ownerId: String, runId: String) = FollowUserEntity(
    ownerId = ownerId,
    userId = restId,
    latestRawJson = toJsonString(),
    name = name,
    screenName = screenName,
    avatarUrl = avatarUrl,
    bannerUrl = bannerUrl,
    bio = bio,
    avatarLocalPath = avatarLocalPath,
    bannerLocalPath = bannerLocalPath,
    runId = runId,
    isFollower = isFollower.asInt(),
    isFollowing = isFollowing.asInt(),
    followerSort = if (isFollower) 1 else null,
    followingSort = if (isFollowing) 1 else null,
)

private fun FollowUserEntity.toRelation() = Relation(
    ownerId = ownerId,
    user = toUser(),
    isFollower = isFollower != 0,
    isFollowing = isFollowing != 0,
    runId = runId,
)

private fun FollowUserEntity.toUser(): TwitterUser =
    latestRawJson.parseUser()?.copy(isFollower = isFollower != 0, isFollowing = isFollowing != 0)
        ?: TwitterUser(
            restId = userId,
            name = name,
            screenName = screenName,
            avatarUrl = avatarUrl,
            bannerUrl = bannerUrl,
            bio = bio,
            avatarLocalPath = avatarLocalPath,
            bannerLocalPath = bannerLocalPath,
            isFollower = isFollower != 0,
            isFollowing = isFollowing != 0,
        )

private fun ChangeReportEntity.toChangeReport() = ChangeReport(
    id = id,
    ownerId = ownerId,
    userId = userId,
    changeType = changeType,
    timestamp = timestamp,
    userSnapshotJson = userSnapshotJson,
)

private fun MediaHistoryEntity.toMediaRecord() = MediaRecord(
    id = id,
    mediaType = mediaType,
    localFilePath = localFilePath,
    remoteUrl = remoteUrl,
    isHighQuality = isHighQuality != 0,
)

private fun MediaRecord.toEntity() = MediaHistoryEntity(
    id = id,
    mediaType = mediaType,
    localFilePath = localFilePath,
    remoteUrl = remoteUrl,
    isHighQuality = isHighQuality.asInt(),
)

private fun String?.parseUser(): TwitterUser? = this?.let { runCatching { TwitterUser.fromJsonString(it) }.getOrNull() }

private fun Boolean.asInt(): Int = if (this) 1 else 0

data class MediaRecord(
    val id: Long,
    val mediaType: String,
    val localFilePath: String,
    val remoteUrl: String,
    val isHighQuality: Boolean,
)

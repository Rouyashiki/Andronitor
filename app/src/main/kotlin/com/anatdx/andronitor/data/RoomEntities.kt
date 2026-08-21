package com.anatdx.andronitor.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "logged_accounts")
data class LoggedAccountEntity(
    @PrimaryKey val id: String,
    val name: String?,
    @ColumnInfo(name = "screen_name") val screenName: String?,
    val bio: String?,
    val location: String?,
    val link: String?,
    @ColumnInfo(name = "join_time") val joinTime: String?,
    @ColumnInfo(name = "is_verified", defaultValue = "0") val isVerified: Int = 0,
    @ColumnInfo(name = "is_protected", defaultValue = "0") val isProtected: Int = 0,
    @ColumnInfo(name = "followers_count", defaultValue = "0") val followersCount: Int = 0,
    @ColumnInfo(name = "following_count", defaultValue = "0") val followingCount: Int = 0,
    @ColumnInfo(name = "statuses_count", defaultValue = "0") val statusesCount: Int = 0,
    @ColumnInfo(name = "media_count", defaultValue = "0") val mediaCount: Int = 0,
    @ColumnInfo(name = "favourites_count", defaultValue = "0") val favouritesCount: Int = 0,
    @ColumnInfo(name = "listed_count", defaultValue = "0") val listedCount: Int = 0,
    @ColumnInfo(name = "latest_raw_json") val latestRawJson: String?,
    @ColumnInfo(name = "avatar_url") val avatarUrl: String?,
    @ColumnInfo(name = "banner_url") val bannerUrl: String?,
    @ColumnInfo(name = "avatar_local_path") val avatarLocalPath: String?,
    @ColumnInfo(name = "banner_local_path") val bannerLocalPath: String?,
)

@Entity(
    tableName = "follow_users",
    primaryKeys = ["owner_id", "user_id"],
)
data class FollowUserEntity(
    @ColumnInfo(name = "owner_id") val ownerId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "latest_raw_json") val latestRawJson: String?,
    val name: String?,
    @ColumnInfo(name = "screen_name") val screenName: String?,
    @ColumnInfo(name = "avatar_url") val avatarUrl: String?,
    @ColumnInfo(name = "banner_url") val bannerUrl: String?,
    val bio: String?,
    @ColumnInfo(name = "avatar_local_path") val avatarLocalPath: String?,
    @ColumnInfo(name = "banner_local_path") val bannerLocalPath: String?,
    @ColumnInfo(name = "run_id") val runId: String?,
    @ColumnInfo(name = "is_follower", defaultValue = "0") val isFollower: Int = 0,
    @ColumnInfo(name = "is_following", defaultValue = "0") val isFollowing: Int = 0,
    @ColumnInfo(name = "follower_sort") val followerSort: Int?,
    @ColumnInfo(name = "following_sort") val followingSort: Int?,
)

@Entity(
    tableName = "follow_users_history",
    indices = [Index(value = ["owner_id", "user_id", "timestamp"], name = "idx_history_owner_user")],
)
data class FollowUserHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "owner_id") val ownerId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "reverse_diff_json") val reverseDiffJson: String,
    @ColumnInfo(name = "run_id") val runId: String?,
    val timestamp: Long,
)

@Entity(tableName = "account_profile_history")
data class AccountProfileHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "owner_id") val ownerId: String,
    @ColumnInfo(name = "reverse_diff_json") val reverseDiffJson: String,
    val timestamp: Long,
)

@Entity(
    tableName = "change_reports",
    indices = [Index(value = ["owner_id", "change_type"], name = "idx_change_reports_owner_type")],
)
data class ChangeReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "owner_id") val ownerId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "change_type") val changeType: String,
    val timestamp: Long,
    @ColumnInfo(name = "user_snapshot_json") val userSnapshotJson: String?,
)

@Entity(
    tableName = "media_history",
    indices = [Index(value = ["remote_url"], name = "index_media_history_remote_url", unique = true)],
)
data class MediaHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "media_type") val mediaType: String,
    @ColumnInfo(name = "local_file_path") val localFilePath: String,
    @ColumnInfo(name = "remote_url") val remoteUrl: String,
    @ColumnInfo(name = "is_high_quality", defaultValue = "0") val isHighQuality: Int = 0,
)

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "run_id") val runId: String,
    val timestamp: Long,
    val status: Int,
    @ColumnInfo(name = "owner_id") val ownerId: String,
)

data class ReportCount(
    @ColumnInfo(name = "change_type") val changeType: String,
    @ColumnInfo(name = "count") val count: Int,
)

@Dao
interface AndronitorDao {
    @Query("SELECT * FROM logged_accounts ORDER BY name COLLATE NOCASE")
    fun getAccounts(): List<LoggedAccountEntity>

    @Query("SELECT * FROM logged_accounts WHERE id = :id LIMIT 1")
    fun getAccount(id: String): LoggedAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAccount(account: LoggedAccountEntity)

    @Query("DELETE FROM logged_accounts WHERE id = :id")
    fun deleteAccount(id: String)

    @Query("SELECT * FROM follow_users WHERE owner_id = :ownerId")
    fun getRelations(ownerId: String): List<FollowUserEntity>

    @Query("SELECT * FROM follow_users WHERE owner_id = :ownerId AND user_id = :userId LIMIT 1")
    fun getRelation(ownerId: String, userId: String): FollowUserEntity?

    /**
     * Rows that could match [query] for the local user search. `latest_raw_json`
     * embeds every searchable field, so this is a superset of the real matches;
     * the exact per-field and attribute filtering happens in Kotlin against the
     * parsed snapshot. SQLite's `lower()` only folds ASCII, so each text test is
     * also run against the raw query for non-ASCII input.
     */
    @Query(
        """
        SELECT * FROM follow_users
        WHERE owner_id = :ownerId
          AND (:followerFilter IS NULL OR is_follower = :followerFilter)
          AND (:followingFilter IS NULL OR is_following = :followingFilter)
          AND (
            :query = ''
            OR user_id = :query
            OR instr(lower(ifnull(name, '')), :loweredQuery) > 0
            OR instr(ifnull(name, ''), :query) > 0
            OR instr(lower(ifnull(screen_name, '')), :loweredQuery) > 0
            OR instr(ifnull(screen_name, ''), :query) > 0
            OR instr(lower(ifnull(bio, '')), :loweredQuery) > 0
            OR instr(ifnull(bio, ''), :query) > 0
            OR instr(lower(ifnull(latest_raw_json, '')), :loweredQuery) > 0
            OR instr(ifnull(latest_raw_json, ''), :query) > 0
          )
        ORDER BY name COLLATE NOCASE, user_id
        """,
    )
    fun searchCandidates(
        ownerId: String,
        query: String,
        loweredQuery: String,
        followerFilter: Int?,
        followingFilter: Int?,
    ): List<FollowUserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertRelation(relation: FollowUserEntity)

    @Query("DELETE FROM follow_users WHERE owner_id = :ownerId AND user_id = :userId")
    fun deleteRelation(ownerId: String, userId: String)

    @Query("DELETE FROM follow_users WHERE owner_id = :ownerId")
    fun deleteRelations(ownerId: String)

    @Insert
    fun insertHistory(history: FollowUserHistoryEntity)

    @Query("DELETE FROM follow_users_history WHERE owner_id = :ownerId")
    fun deleteHistory(ownerId: String)

    @Query("DELETE FROM account_profile_history WHERE owner_id = :ownerId")
    fun deleteProfileHistory(ownerId: String)

    @Insert
    fun insertReport(report: ChangeReportEntity): Long

    @Query("SELECT * FROM change_reports WHERE owner_id = :ownerId AND user_id = :userId ORDER BY timestamp DESC")
    fun getReports(ownerId: String, userId: String): List<ChangeReportEntity>

    @Query("SELECT * FROM change_reports WHERE owner_id = :ownerId AND change_type = :category ORDER BY timestamp DESC, id DESC")
    fun getReportsForCategory(ownerId: String, category: String): List<ChangeReportEntity>

    // Distinct users, not raw rows: a person who changed N times is one entry in
    // the category list (see getUsersForCategory), so the badge must match.
    @Query("SELECT change_type, COUNT(DISTINCT user_id) AS count FROM change_reports WHERE owner_id = :ownerId GROUP BY change_type")
    fun getReportCounts(ownerId: String): List<ReportCount>

    @Query("DELETE FROM change_reports WHERE owner_id = :ownerId")
    fun deleteReports(ownerId: String)

    @Query("SELECT * FROM follow_users WHERE owner_id = :ownerId AND is_follower = 1 ORDER BY follower_sort, name COLLATE NOCASE")
    fun getFollowers(ownerId: String): List<FollowUserEntity>

    @Query("SELECT * FROM follow_users WHERE owner_id = :ownerId AND is_following = 1 ORDER BY following_sort, name COLLATE NOCASE")
    fun getFollowing(ownerId: String): List<FollowUserEntity>

    @Query("SELECT * FROM media_history WHERE remote_url = :remoteUrl ORDER BY id DESC LIMIT 1")
    fun getMediaRecord(remoteUrl: String): MediaHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertMediaRecord(record: MediaHistoryEntity)

    @Insert
    fun insertSyncLog(log: SyncLogEntity)

    @Query("SELECT timestamp FROM sync_logs WHERE owner_id = :ownerId ORDER BY timestamp DESC LIMIT 1")
    fun getLastSync(ownerId: String): Long?

    @Query("DELETE FROM sync_logs WHERE owner_id = :ownerId")
    fun deleteSyncLogs(ownerId: String)
}

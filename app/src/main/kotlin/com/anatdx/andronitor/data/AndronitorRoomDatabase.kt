package com.anatdx.andronitor.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        LoggedAccountEntity::class,
        FollowUserEntity::class,
        FollowUserHistoryEntity::class,
        AccountProfileHistoryEntity::class,
        ChangeReportEntity::class,
        MediaHistoryEntity::class,
        SyncLogEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AndronitorRoomDatabase : RoomDatabase() {
    abstract fun dao(): AndronitorDao
}

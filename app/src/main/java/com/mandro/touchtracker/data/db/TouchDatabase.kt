package com.mandro.touchtracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mandro.touchtracker.data.db.entity.SessionEntity
import com.mandro.touchtracker.data.db.entity.TouchPointEntity

@Database(
    entities = [SessionEntity::class, TouchPointEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TouchDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun touchPointDao(): TouchPointDao

    companion object {
        const val NAME = "touch_tracker.db"
    }
}

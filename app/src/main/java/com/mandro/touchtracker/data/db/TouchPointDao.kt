package com.mandro.touchtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mandro.touchtracker.data.db.entity.TouchPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TouchPointDao {

    @Query("SELECT * FROM touch_points WHERE session_id = :sessionId ORDER BY sequence ASC")
    fun observeBySession(sessionId: Long): Flow<List<TouchPointEntity>>

    /**
     * 내보내기용 페이지 읽기. 수만 점짜리 세션을 통째로 메모리에 올리지 않으려고 쓴다.
     */
    @Query(
        "SELECT * FROM touch_points WHERE session_id = :sessionId " +
            "ORDER BY sequence ASC LIMIT :limit OFFSET :offset",
    )
    suspend fun getPage(sessionId: Long, limit: Int, offset: Int): List<TouchPointEntity>

    @Query("SELECT * FROM touch_points WHERE session_id = :sessionId ORDER BY sequence ASC")
    suspend fun getBySession(sessionId: Long): List<TouchPointEntity>

    @Insert
    suspend fun insertAll(points: List<TouchPointEntity>)

    @Query("SELECT COUNT(*) FROM touch_points WHERE session_id = :sessionId")
    suspend fun countBySession(sessionId: Long): Int
}

package com.mandro.touchtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mandro.touchtracker.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

/** 세션 행 + 점 개수를 한 번에 읽기 위한 조인 결과. */
data class SessionWithCount(
    @androidx.room.Embedded val session: SessionEntity,
    @androidx.room.ColumnInfo(name = "point_count") val pointCount: Int,
)

@Dao
interface SessionDao {

    /**
     * 최근 세션이 위. 점 개수는 LEFT JOIN 집계로 한 방에 가져온다
     * (세션마다 COUNT 쿼리를 도는 N+1 을 피한다).
     */
    @Query(
        """
        SELECT s.*, COUNT(p.id) AS point_count
        FROM sessions AS s
        LEFT JOIN touch_points AS p ON p.session_id = s.id
        GROUP BY s.id
        ORDER BY s.started_at DESC
        """,
    )
    fun observeAll(): Flow<List<SessionWithCount>>

    @Query(
        """
        SELECT s.*, COUNT(p.id) AS point_count
        FROM sessions AS s
        LEFT JOIN touch_points AS p ON p.session_id = s.id
        WHERE s.id = :sessionId
        GROUP BY s.id
        """,
    )
    fun observeById(sessionId: Long): Flow<SessionWithCount?>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getById(sessionId: Long): SessionEntity?

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("UPDATE sessions SET ended_at = :endedAtEpochMs WHERE id = :sessionId AND ended_at IS NULL")
    suspend fun markEnded(sessionId: Long, endedAtEpochMs: Long)

    @Query("UPDATE sessions SET ended_at = NULL WHERE id = :sessionId")
    suspend fun reopen(sessionId: Long)

    @Query("UPDATE sessions SET name = :name, note = :note WHERE id = :sessionId")
    suspend fun rename(sessionId: Long, name: String, note: String)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun delete(sessionId: Long)
}

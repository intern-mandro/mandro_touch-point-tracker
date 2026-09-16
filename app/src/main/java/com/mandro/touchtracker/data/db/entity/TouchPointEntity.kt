package com.mandro.touchtracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mandro.touchtracker.model.TouchPhase
import com.mandro.touchtracker.model.TouchPoint

/**
 * 세션이 지워지면 점도 같이 지워진다(CASCADE). 고아 데이터가 남으면
 * 좌표계를 모르는 px 값 뭉치가 되어 아무 의미가 없다.
 */
@Entity(
    tableName = "touch_points",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["session_id", "sequence"])],
)
data class TouchPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    val sequence: Int,
    @ColumnInfo(name = "pointer_id") val pointerId: Int,
    /** [TouchPhase] 의 이름. enum ordinal 을 쓰면 상수 순서만 바뀌어도 과거 데이터가 깨진다. */
    val phase: String,
    @ColumnInfo(name = "x_px") val xPx: Float,
    @ColumnInfo(name = "y_px") val yPx: Float,
    val pressure: Float,
    @ColumnInfo(name = "touch_major_px") val touchMajorPx: Float,
    @ColumnInfo(name = "touch_minor_px") val touchMinorPx: Float,
    @ColumnInfo(name = "orientation_rad") val orientationRad: Float,
    @ColumnInfo(name = "elapsed_ms") val elapsedMs: Long,
    @ColumnInfo(name = "epoch_ms") val epochMs: Long,
)

fun TouchPointEntity.toDomain() = TouchPoint(
    id = id,
    sessionId = sessionId,
    sequence = sequence,
    pointerId = pointerId,
    phase = runCatching { TouchPhase.valueOf(phase) }.getOrDefault(TouchPhase.CANCEL),
    xPx = xPx,
    yPx = yPx,
    pressure = pressure,
    touchMajorPx = touchMajorPx,
    touchMinorPx = touchMinorPx,
    orientationRad = orientationRad,
    elapsedMs = elapsedMs,
    epochMs = epochMs,
)

fun TouchPoint.toEntity() = TouchPointEntity(
    id = id,
    sessionId = sessionId,
    sequence = sequence,
    pointerId = pointerId,
    phase = phase.name,
    xPx = xPx,
    yPx = yPx,
    pressure = pressure,
    touchMajorPx = touchMajorPx,
    touchMinorPx = touchMinorPx,
    orientationRad = orientationRad,
    elapsedMs = elapsedMs,
    epochMs = epochMs,
)

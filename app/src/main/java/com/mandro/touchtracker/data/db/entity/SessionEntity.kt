package com.mandro.touchtracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mandro.touchtracker.core.geometry.ScreenMetrics
import com.mandro.touchtracker.model.DeviceProfile
import com.mandro.touchtracker.model.TouchSession

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val note: String,
    @ColumnInfo(name = "started_at") val startedAtEpochMs: Long,
    @ColumnInfo(name = "ended_at") val endedAtEpochMs: Long?,

    // ── 측정 당시 좌표계. 세션이 만들어질 때 박제되고 이후 바뀌지 않는다. ──
    @ColumnInfo(name = "device_model") val deviceModel: String,
    @ColumnInfo(name = "device_manufacturer") val deviceManufacturer: String,
    @ColumnInfo(name = "android_sdk") val androidSdk: Int,
    @ColumnInfo(name = "width_px") val widthPx: Int,
    @ColumnInfo(name = "height_px") val heightPx: Int,
    @ColumnInfo(name = "x_dpi") val xDpi: Float,
    @ColumnInfo(name = "y_dpi") val yDpi: Float,
    val density: Float,
)

fun SessionEntity.toDomain(pointCount: Int = 0) = TouchSession(
    id = id,
    name = name,
    note = note,
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = endedAtEpochMs,
    device = DeviceProfile(
        model = deviceModel,
        manufacturer = deviceManufacturer,
        androidSdk = androidSdk,
        metrics = ScreenMetrics(
            widthPx = widthPx,
            heightPx = heightPx,
            xDpi = xDpi,
            yDpi = yDpi,
            density = density,
        ),
    ),
    pointCount = pointCount,
)

fun TouchSession.toEntity() = SessionEntity(
    id = id,
    name = name,
    note = note,
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = endedAtEpochMs,
    deviceModel = device.model,
    deviceManufacturer = device.manufacturer,
    androidSdk = device.androidSdk,
    widthPx = device.metrics.widthPx,
    heightPx = device.metrics.heightPx,
    xDpi = device.metrics.xDpi,
    yDpi = device.metrics.yDpi,
    density = device.metrics.density,
)

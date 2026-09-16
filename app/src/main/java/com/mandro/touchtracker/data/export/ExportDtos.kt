package com.mandro.touchtracker.data.export

import com.mandro.touchtracker.model.TouchPoint
import com.mandro.touchtracker.model.TouchSession
import kotlinx.serialization.Serializable

/**
 * JSON 내보내기 스키마.
 *
 * 앱 내부 모델을 그대로 직렬화하지 않는다 — 내부 모델은 리팩터링으로 바뀌지만
 * 내보낸 파일은 분석 스크립트가 붙잡고 있는 계약이기 때문이다. 필드를 바꾸면
 * [FORMAT_VERSION] 을 올린다.
 */
const val FORMAT_VERSION = 1

@Serializable
data class SessionMetaDto(
    val id: Long,
    val name: String,
    val note: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val deviceManufacturer: String,
    val deviceModel: String,
    val androidSdk: Int,
    val widthPx: Int,
    val heightPx: Int,
    val xDpi: Float,
    val yDpi: Float,
    val density: Float,
    /** DPI 를 믿을 수 없는 기기면 false — 이때 mm 값은 px 와 같은 수이므로 쓰면 안 된다. */
    val hasPhysicalDpi: Boolean,
)

@Serializable
data class TouchPointDto(
    val sequence: Int,
    val pointerId: Int,
    val phase: String,
    val xPx: Float,
    val yPx: Float,
    val xMm: Float,
    val yMm: Float,
    val xNorm: Float,
    val yNorm: Float,
    val pressure: Float,
    val touchMajorPx: Float,
    val touchMinorPx: Float,
    val orientationRad: Float,
    val elapsedMs: Long,
    val epochMs: Long,
)

fun TouchSession.toMetaDto() = SessionMetaDto(
    id = id,
    name = name,
    note = note,
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = endedAtEpochMs,
    deviceManufacturer = device.manufacturer,
    deviceModel = device.model,
    androidSdk = device.androidSdk,
    widthPx = metrics.widthPx,
    heightPx = metrics.heightPx,
    xDpi = metrics.xDpi,
    yDpi = metrics.yDpi,
    density = metrics.density,
    hasPhysicalDpi = metrics.hasPhysicalDpi,
)

fun TouchPoint.toDto(session: TouchSession) = TouchPointDto(
    sequence = sequence,
    pointerId = pointerId,
    phase = phase.name,
    xPx = xPx,
    yPx = yPx,
    xMm = xMm(session.metrics),
    yMm = yMm(session.metrics),
    xNorm = xNormalized(session.metrics),
    yNorm = yNormalized(session.metrics),
    pressure = pressure,
    touchMajorPx = touchMajorPx,
    touchMinorPx = touchMinorPx,
    orientationRad = orientationRad,
    elapsedMs = elapsedMs,
    epochMs = epochMs,
)

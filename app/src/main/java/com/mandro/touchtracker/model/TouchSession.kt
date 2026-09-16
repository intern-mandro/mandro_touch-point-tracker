package com.mandro.touchtracker.model

import com.mandro.touchtracker.core.geometry.ScreenMetrics

/**
 * 측정이 일어난 기기와 화면. 세션마다 통째로 박제해서, 나중에 데이터만 봐도
 * "어떤 좌표계에서 잰 값인지" 가 남게 한다.
 */
data class DeviceProfile(
    val model: String,
    val manufacturer: String,
    val androidSdk: Int,
    val metrics: ScreenMetrics,
) {
    companion object {
        val PREVIEW = DeviceProfile(
            model = "Preview", manufacturer = "mandro",
            androidSdk = 35, metrics = ScreenMetrics.PREVIEW,
        )
    }
}

/**
 * 실험 1회차. 로봇 손의 한 조준 조건(자세·목표점·속도 등)당 하나를 만든다.
 *
 * @param endedAtEpochMs null 이면 아직 기록 중인 세션.
 * @param pointCount     DB 집계값. 매번 세지 않으려고 들고 다닌다.
 * @param note           실험 조건 메모 — 어떤 조건이었는지는 사람만 안다.
 */
data class TouchSession(
    val id: Long = NO_ID,
    val name: String,
    val note: String = "",
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val device: DeviceProfile,
    val pointCount: Int = 0,
) {
    val isRecording: Boolean get() = endedAtEpochMs == null

    val metrics: ScreenMetrics get() = device.metrics

    companion object {
        const val NO_ID = 0L
    }
}

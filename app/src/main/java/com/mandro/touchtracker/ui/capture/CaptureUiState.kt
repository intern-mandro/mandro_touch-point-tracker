package com.mandro.touchtracker.ui.capture

import com.mandro.touchtracker.core.geometry.ScreenMetrics
import com.mandro.touchtracker.model.CaptureSettings
import com.mandro.touchtracker.model.TouchPoint

/**
 * 캡처 화면 한 장이 필요로 하는 전부. 두 탭이 이 하나를 나눠 본다 —
 * 탭마다 상태를 따로 두면 같은 터치를 서로 다르게 보는 순간이 생긴다.
 */
data class CaptureUiState(
    val isRecording: Boolean = false,
    /**
     * 녹화 버튼으로 시작한 세션인가. 그냥 화면을 눌러 자동으로 열린 세션은 false 다.
     * 상단바가 "녹화 중" 표시등을 띄울지 초기화 버튼을 띄울지 가른다.
     */
    val isExplicitRecording: Boolean = false,
    val sessionName: String = "",
    val settings: CaptureSettings = CaptureSettings.DEFAULT,
    val metrics: ScreenMetrics = ScreenMetrics.PREVIEW,
    /** 오래된 것부터. 모눈종이 탭이 궤적을 그리는 순서다. */
    val livePoints: List<TouchPoint> = emptyList(),
    val message: String? = null,
) {
    /** 데이터 탭용 — **최신이 맨 위**. 화면 맨 위가 방금 찍힌 점이어야 눈이 덜 움직인다. */
    val recentPoints: List<TouchPoint>
        get() = livePoints.takeLast(CaptureSettings.RECENT_POINT_LIMIT).asReversed()

    /** 모눈종이 탭용 — 화면에 표시할 최근 점 목록 (최대 9개 FIFO 큐). 오래된 것부터 최신 순. */
    val recentLivePoints: List<TouchPoint>
        get() = livePoints.takeLast(CaptureSettings.RECENT_POINT_LIMIT)

    val latestPoint: TouchPoint? get() = livePoints.lastOrNull()

    val hasPoints: Boolean get() = livePoints.isNotEmpty()
}

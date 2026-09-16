package com.mandro.touchtracker.data.touch

import com.mandro.touchtracker.model.TouchPhase

/**
 * MotionEvent 한 개에서 뽑아낸 원시 샘플 1건. 아직 세션에 속하지 않았다 —
 * sessionId·sequence 는 [TouchCaptureController] 가 붙인다.
 *
 * @param eventTimeUptimeMs MotionEvent.eventTime. 단조 증가 시계 기준이라
 *                          세션 시작 시각과 빼면 그대로 경과 시간이 된다.
 */
data class TouchSample(
    val pointerId: Int,
    val phase: TouchPhase,
    val xPx: Float,
    val yPx: Float,
    val pressure: Float,
    val touchMajorPx: Float,
    val touchMinorPx: Float,
    val orientationRad: Float,
    val eventTimeUptimeMs: Long,
)

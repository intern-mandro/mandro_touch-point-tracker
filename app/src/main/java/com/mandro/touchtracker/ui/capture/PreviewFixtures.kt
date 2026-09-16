package com.mandro.touchtracker.ui.capture

import com.mandro.touchtracker.model.TouchPhase
import com.mandro.touchtracker.model.TouchPoint
import kotlin.math.cos
import kotlin.math.sin

/**
 * @Preview 전용 가짜 데이터. 실제 코드 경로에서는 쓰이지 않는다.
 *
 * 로봇 손이 한 점을 반복해 노리는 상황을 흉내 낸다 — 목표점 주변에 살짝씩
 * 어긋나 찍히는 모양이라야 모눈종이 탭의 흩어짐 표현을 제대로 확인할 수 있다.
 */
internal fun previewTouchPoints(count: Int = 9): List<TouchPoint> {
    val targetX = 540f
    val targetY = 1100f
    return List(count) { index ->
        val angle = index * GOLDEN_ANGLE_RAD
        val radius = SPREAD_PX * (index + 1) / count
        TouchPoint(
            sessionId = 1L,
            sequence = index,
            pointerId = 0,
            phase = TouchPhase.DOWN,
            xPx = targetX + cos(angle) * radius,
            yPx = targetY + sin(angle) * radius,
            pressure = 0.42f + index * 0.01f,
            touchMajorPx = 38f + index,
            touchMinorPx = 32f + index,
            orientationRad = 0f,
            elapsedMs = index * 820L,
            epochMs = 1_757_000_000_000L + index * 820L,
        )
    }
}

/** 황금각으로 돌리면 적은 개수로도 방향이 겹치지 않고 고르게 퍼진다. */
private const val GOLDEN_ANGLE_RAD = 2.399963f
private const val SPREAD_PX = 70f

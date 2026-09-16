package com.mandro.touchtracker.model

import com.mandro.touchtracker.core.geometry.ScreenMetrics

/** MotionEvent 한 샘플이 터치 생애주기의 어느 단계인지. */
enum class TouchPhase {
    /** 손끝이 화면에 닿은 순간. 로봇 손의 "조준 결과"는 보통 이 점이다. */
    DOWN,

    /** 닿은 채로 움직이는 중 (설정에서 켰을 때만 기록). */
    MOVE,

    /** 떨어진 순간. DOWN 과의 거리가 곧 접촉 중 미끄러짐(slip). */
    UP,

    /** 시스템이 제스처를 가로채 취소시킴. 측정값으로 쓰면 안 된다. */
    CANCEL,
}

/**
 * 로봇 손이 화면을 눌렀을 때 화면이 실제로 관측한 한 점.
 *
 * 좌표는 **캡처 화면 좌상단 기준 px** 로만 저장한다. mm·정규화 좌표는 저장하지 않고
 * 세션의 [ScreenMetrics] 로 그때그때 환산한다 — 중복 저장은 둘이 어긋날 여지만 만든다.
 *
 * @param sequence  세션 안에서 0부터 증가. 정렬 키이자 UI 의 "#N".
 * @param pointerId 멀티터치 손가락 구분. 로봇 손가락 여러 개가 동시에 닿을 때 갈라 본다.
 * @param pressure  MotionEvent.getPressure(). 보통 0f..1f 지만 기기마다 스케일이
 *                  제각각이라 **절대값 비교 금지**, 같은 기기 안 상대 비교만 유효하다.
 * @param touchMajorPx 접촉 타원의 장축 (MotionEvent.getTouchMajor). 눌린 면적의 대리 지표.
 * @param touchMinorPx 접촉 타원의 단축.
 * @param orientationRad 접촉 타원의 기울기 (rad). 지원 안 하는 기기는 0f.
 * @param elapsedMs 세션 시작 기준 경과 시간. MotionEvent.eventTime(단조 시계) 기반.
 * @param epochMs   절대 시각. 외부 로그(로봇 컨트롤러 로그 등)와 맞출 때 쓴다.
 */
data class TouchPoint(
    val id: Long = NO_ID,
    val sessionId: Long,
    val sequence: Int,
    val pointerId: Int,
    val phase: TouchPhase,
    val xPx: Float,
    val yPx: Float,
    val pressure: Float,
    val touchMajorPx: Float,
    val touchMinorPx: Float,
    val orientationRad: Float,
    val elapsedMs: Long,
    val epochMs: Long,
) {
    fun xMm(metrics: ScreenMetrics): Float = metrics.toMmX(xPx)
    fun yMm(metrics: ScreenMetrics): Float = metrics.toMmY(yPx)

    fun xNormalized(metrics: ScreenMetrics): Float = metrics.toNormalizedX(xPx)
    fun yNormalized(metrics: ScreenMetrics): Float = metrics.toNormalizedY(yPx)

    companion object {
        /** 아직 DB 에 안 들어간 점. Room 이 autoGenerate 로 진짜 id 를 채운다. */
        const val NO_ID = 0L
    }
}

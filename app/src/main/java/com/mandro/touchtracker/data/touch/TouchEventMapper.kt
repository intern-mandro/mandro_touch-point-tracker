package com.mandro.touchtracker.data.touch

import android.view.InputDevice
import android.view.MotionEvent
import com.mandro.touchtracker.model.TouchPhase

/**
 * MotionEvent → [TouchSample] 목록.
 *
 * 안드로이드 터치 이벤트의 함정 두 가지를 여기서 흡수한다.
 *
 * 1. **묶음 전달(batching).** ACTION_MOVE 한 개 안에 과거 샘플이 여러 개 들어 있다
 *    (`historySize`). 이걸 안 읽으면 60Hz 로 샘플이 깎여서, 터치 궤적의 실제
 *    해상도(보통 120~240Hz)를 잃는다. 조준 정밀도를 재는 앱에서는 치명적이다.
 * 2. **멀티 포인터.** DOWN/UP 은 `actionIndex` 의 한 포인터만, MOVE 는 모든
 *    포인터를 한꺼번에 나른다. 두 경우를 따로 풀어야 한다.
 */
object TouchEventMapper {

    /**
     * @param ignoreSynthetic true 면 물리 터치스크린이 아닌 입력(주입된 이벤트 등)을 버린다.
     * @return 이벤트 시간 순서대로 정렬된 샘플. 버릴 이벤트면 빈 목록.
     */
    fun map(event: MotionEvent, ignoreSynthetic: Boolean): List<TouchSample> {
        if (ignoreSynthetic && !isPhysicalTouch(event)) return emptyList()

        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            -> listOf(sampleAt(event, event.actionIndex, TouchPhase.DOWN))

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            -> listOf(sampleAt(event, event.actionIndex, TouchPhase.UP))

            MotionEvent.ACTION_CANCEL ->
                (0 until event.pointerCount).map { sampleAt(event, it, TouchPhase.CANCEL) }

            MotionEvent.ACTION_MOVE -> moveSamples(event)

            // HOVER/SCROLL/BUTTON_PRESS 등은 화면 접촉이 아니다 — 측정 대상 아님.
            else -> emptyList()
        }
    }

    /** 과거 샘플(오래된 것부터) → 현재 샘플 순으로 편다. */
    private fun moveSamples(event: MotionEvent): List<TouchSample> {
        val out = ArrayList<TouchSample>((event.historySize + 1) * event.pointerCount)
        for (h in 0 until event.historySize) {
            for (i in 0 until event.pointerCount) {
                out += historicalSampleAt(event, i, h)
            }
        }
        for (i in 0 until event.pointerCount) {
            out += sampleAt(event, i, TouchPhase.MOVE)
        }
        return out
    }

    private fun sampleAt(event: MotionEvent, index: Int, phase: TouchPhase) = TouchSample(
        pointerId = event.getPointerId(index),
        phase = phase,
        xPx = event.getX(index),
        yPx = event.getY(index),
        pressure = event.getPressure(index),
        touchMajorPx = event.getTouchMajor(index),
        touchMinorPx = event.getTouchMinor(index),
        orientationRad = event.getOrientation(index),
        eventTimeUptimeMs = event.eventTime,
    )

    private fun historicalSampleAt(event: MotionEvent, index: Int, pos: Int) = TouchSample(
        pointerId = event.getPointerId(index),
        phase = TouchPhase.MOVE,
        xPx = event.getHistoricalX(index, pos),
        yPx = event.getHistoricalY(index, pos),
        pressure = event.getHistoricalPressure(index, pos),
        touchMajorPx = event.getHistoricalTouchMajor(index, pos),
        touchMinorPx = event.getHistoricalTouchMinor(index, pos),
        orientationRad = event.getHistoricalOrientation(index, pos),
        eventTimeUptimeMs = event.getHistoricalEventTime(pos),
    )

    /**
     * 진짜 터치스크린에서 온 이벤트인지.
     *
     * `adb shell input tap` 이나 접근성 서비스가 주입한 이벤트는 실제 디바이스가 없어
     * deviceId 가 0 이하로 온다. 공개 API 에 "주입됨" 플래그가 없어서 이 휴리스틱을 쓴다.
     * 실측 세션에 사람 손이나 스크립트 탭이 섞여 들어가는 사고를 막는 게 목적이다.
     */
    fun isPhysicalTouch(event: MotionEvent): Boolean =
        event.deviceId > 0 && event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN)

}

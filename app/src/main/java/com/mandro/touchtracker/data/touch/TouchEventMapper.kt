package com.mandro.touchtracker.data.touch

import android.view.InputDevice
import android.view.MotionEvent
import com.mandro.touchtracker.model.TouchPhase

object TouchEventMapper {

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

    // 현재 샘플 순서로 핀다
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

    fun isPhysicalTouch(event: MotionEvent): Boolean =
        event.deviceId > 0 && event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN)

}

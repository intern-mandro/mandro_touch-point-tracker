package com.mandro.touchtracker.ui.capture

import com.mandro.touchtracker.core.geometry.ScreenMetrics
import com.mandro.touchtracker.model.CoordinateUnit
import com.mandro.touchtracker.model.TouchPoint
import java.util.Locale

/**
 * 좌표를 사람이 읽는 문자열로. 화면 어디서든 같은 자릿수로 보이게 여기 한 곳에 모은다.
 *
 * 자릿수는 단위별로 다르다 — px 는 소수 1자리면 충분하지만(터치 해상도가 그 이하),
 * 정규화 좌표는 4자리를 줘야 화면 폭 2000px 기준 1px 를 구분할 수 있다.
 */
object CoordinateFormat {

    fun x(point: TouchPoint, metrics: ScreenMetrics, unit: CoordinateUnit): String =
        when (unit) {
            CoordinateUnit.PX -> format(point.xPx, PX_DECIMALS)
            CoordinateUnit.MM -> format(point.xMm(metrics), MM_DECIMALS)
            CoordinateUnit.NORMALIZED -> format(point.xNormalized(metrics), NORM_DECIMALS)
        }

    fun y(point: TouchPoint, metrics: ScreenMetrics, unit: CoordinateUnit): String =
        when (unit) {
            CoordinateUnit.PX -> format(point.yPx, PX_DECIMALS)
            CoordinateUnit.MM -> format(point.yMm(metrics), MM_DECIMALS)
            CoordinateUnit.NORMALIZED -> format(point.yNormalized(metrics), NORM_DECIMALS)
        }

    /**
     * 단위 꼬리표. mm 인데 기기 DPI 를 못 믿으면 `mm?` 로 표시해서,
     * 그 값이 사실은 환산되지 않은 px 라는 걸 숨기지 않는다.
     */
    fun unitLabel(unit: CoordinateUnit, metrics: ScreenMetrics): String = when (unit) {
        CoordinateUnit.PX -> "px"
        CoordinateUnit.MM -> if (metrics.hasPhysicalDpi) "mm" else "mm?"
        CoordinateUnit.NORMALIZED -> ""
    }

    fun pressure(value: Float): String = format(value, PRESSURE_DECIMALS)

    /** 접촉 타원의 장축. 눌린 면적이 얼마나 컸는지의 대리 지표다. */
    fun contactSize(point: TouchPoint): String = format(point.touchMajorPx, PX_DECIMALS)

    fun elapsed(elapsedMs: Long): String = when {
        elapsedMs < MS_PER_SECOND -> "${elapsedMs}ms"
        else -> String.format(Locale.US, "%.2fs", elapsedMs / MS_PER_SECOND.toFloat())
    }

    private fun format(value: Float, decimals: Int): String =
        if (value.isFinite()) String.format(Locale.US, "%.${decimals}f", value) else "—"

    private const val PX_DECIMALS = 1
    private const val MM_DECIMALS = 2
    private const val NORM_DECIMALS = 4
    private const val PRESSURE_DECIMALS = 3
    private const val MS_PER_SECOND = 1000L
}

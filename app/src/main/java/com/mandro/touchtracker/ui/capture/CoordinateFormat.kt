package com.mandro.touchtracker.ui.capture

import com.mandro.touchtracker.core.geometry.ScreenMetrics
import com.mandro.touchtracker.model.CoordinateUnit
import com.mandro.touchtracker.model.TouchPoint
import java.util.Locale

import kotlin.math.roundToInt

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

    /**
     * 화면 기준 해상도 및 치수 안내 문자열.
     * 사용자가 측정 좌표를 볼 때 전체 화면의 기준 크기가 몇 바이 몇인지 바로 확인할 수 있게 한다.
     */
    fun referenceLabel(unit: CoordinateUnit, metrics: ScreenMetrics): String = when (unit) {
        CoordinateUnit.PX -> "${metrics.widthPx} × ${metrics.heightPx} px"
        CoordinateUnit.MM -> if (metrics.hasPhysicalDpi) {
            val widthMm = metrics.toMmX(metrics.widthPx.toFloat()).roundToInt()
            val heightMm = metrics.toMmY(metrics.heightPx.toFloat()).roundToInt()
            "${widthMm} × ${heightMm} mm"
        } else {
            "${metrics.widthPx} × ${metrics.heightPx} px (DPI 미지원)"
        }
        CoordinateUnit.NORMALIZED -> "0.00 ~ 1.00"
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

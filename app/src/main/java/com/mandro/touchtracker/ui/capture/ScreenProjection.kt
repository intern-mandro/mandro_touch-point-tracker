package com.mandro.touchtracker.ui.capture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.mandro.touchtracker.core.geometry.ScreenMetrics

/**
 * 기기 전체 화면(px) → 모눈종이 캔버스(px) 사영.
 *
 * 모눈종이 탭은 탭 바 아래 일부만 차지하지만 **화면 전체**를 보여준다. 로봇이 화면
 * 어디를 눌렀는지가 관심사이므로, 캔버스에 화면 전체를 비율 유지한 채 맞춰 넣고
 * 남는 쪽을 여백으로 둔다 (letterbox). 비율을 안 지키면 x/y 축 왜곡 때문에
 * 점이 흩어진 모양 자체가 거짓말이 된다.
 */
data class ScreenProjection(
    val metrics: ScreenMetrics,
    val scale: Float,
    val originX: Float,
    val originY: Float,
) {
    fun toCanvasX(xPx: Float): Float = originX + xPx * scale

    fun toCanvasY(yPx: Float): Float = originY + yPx * scale

    fun toCanvas(xPx: Float, yPx: Float): Offset = Offset(toCanvasX(xPx), toCanvasY(yPx))

    /** 사영된 화면 사각형의 크기. 테두리와 격자는 이 안에만 그린다. */
    val projectedWidth: Float get() = metrics.widthPx * scale

    val projectedHeight: Float get() = metrics.heightPx * scale

    /** mm 단위 길이를 캔버스 px 로. 모눈 한 칸 크기 계산에 쓴다. */
    fun mmToCanvas(mm: Float): Float = metrics.toPxX(mm) * scale

    companion object {
        /**
         * 1:1 직접 매핑.
         * 캔버스가 윈도우 내 (canvasOffsetInWindow)에 위치할 때,
         * 윈도우 터치 좌표 (xPx, yPx)를 캔버스 로컬 좌표로 1:1 변환하여
         * 손가락이 닿은 바로 그 자리에 정확히 그려지도록 한다.
         */
        fun direct(metrics: ScreenMetrics, canvasOffsetInWindow: Offset = Offset.Zero): ScreenProjection {
            return ScreenProjection(
                metrics = metrics,
                scale = 1f,
                originX = -canvasOffsetInWindow.x,
                originY = -canvasOffsetInWindow.y,
            )
        }

        fun fit(metrics: ScreenMetrics, canvasSize: Size): ScreenProjection {
            val scale = minOf(
                canvasSize.width / metrics.widthPx,
                canvasSize.height / metrics.heightPx,
            )
            return ScreenProjection(
                metrics = metrics,
                scale = scale,
                originX = (canvasSize.width - metrics.widthPx * scale) / 2f,
                originY = (canvasSize.height - metrics.heightPx * scale) / 2f,
            )
        }
    }
}

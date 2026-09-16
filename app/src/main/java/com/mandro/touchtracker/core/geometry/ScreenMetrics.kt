package com.mandro.touchtracker.core.geometry

/**
 * 한 세션이 측정된 화면의 좌표계.
 *
 * px 값은 기기·해상도가 바뀌면 그대로 비교할 수 없다. 그래서 세션마다 이 값을 통째로
 * 박제해 두고, 내보낸 데이터에도 같이 실어 보낸다. 나중에 다른 기기에서 뽑은 데이터와
 * 비교할 때는 px 가 아니라 [toMmX] / [toMmY] 로 환산한 mm 를 쓴다.
 *
 * @param widthPx  캡처 영역 가로 (px)
 * @param heightPx 캡처 영역 세로 (px)
 * @param xDpi     가로 방향 실제 물리 DPI (DisplayMetrics.xdpi)
 * @param yDpi     세로 방향 실제 물리 DPI (DisplayMetrics.ydpi)
 * @param density  논리 밀도 (dp 환산용). 물리 치수 계산에는 쓰지 않는다.
 */
data class ScreenMetrics(
    val widthPx: Int,
    val heightPx: Int,
    val xDpi: Float,
    val yDpi: Float,
    val density: Float,
) {
    init {
        require(widthPx > 0 && heightPx > 0) { "화면 크기는 0보다 커야 한다: ${widthPx}x$heightPx" }
    }

    /** DPI 를 못 믿을 기기가 있다. 0 이하이면 mm 환산 대신 px 를 그대로 쓴다. */
    val hasPhysicalDpi: Boolean get() = xDpi > 0f && yDpi > 0f

    fun toMmX(xPx: Float): Float = if (hasPhysicalDpi) xPx / xDpi * MM_PER_INCH else xPx
    fun toMmY(yPx: Float): Float = if (hasPhysicalDpi) yPx / yDpi * MM_PER_INCH else yPx

    fun toPxX(xMm: Float): Float = if (hasPhysicalDpi) xMm / MM_PER_INCH * xDpi else xMm
    fun toPxY(yMm: Float): Float = if (hasPhysicalDpi) yMm / MM_PER_INCH * yDpi else yMm

    /** 0f..1f 로 정규화. 기기가 달라도 "화면 어디쯤" 은 비교할 수 있다. */
    fun toNormalizedX(xPx: Float): Float = xPx / widthPx
    fun toNormalizedY(yPx: Float): Float = yPx / heightPx

    companion object {
        const val MM_PER_INCH = 25.4f

        /** 프리뷰·테스트용 기본값 (대략 FHD+ 6.1" 폰). */
        val PREVIEW = ScreenMetrics(
            widthPx = 1080, heightPx = 2340,
            xDpi = 420f, yDpi = 420f, density = 2.625f,
        )
    }
}

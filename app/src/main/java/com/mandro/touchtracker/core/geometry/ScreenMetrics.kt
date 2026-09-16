package com.mandro.touchtracker.core.geometry
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

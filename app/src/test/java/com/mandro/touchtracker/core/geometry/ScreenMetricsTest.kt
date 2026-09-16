package com.mandro.touchtracker.core.geometry

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScreenMetricsTest {

    private val metrics = ScreenMetrics(
        widthPx = 1000,
        heightPx = 2000,
        xDpi = 254f, // 10 px = 1 mm 로 딱 떨어지는 값
        yDpi = 508f, // 20 px = 1 mm
        density = 2.5f,
    )

    @Test
    fun `converts px to mm using the axis specific dpi`() {
        // Arrange — x/y 의 DPI 가 다른 기기를 가정한다.
        val xPx = 100f
        val yPx = 100f

        // Act
        val xMm = metrics.toMmX(xPx)
        val yMm = metrics.toMmY(yPx)

        // Assert — 같은 px 라도 축이 다르면 물리 길이가 다르다.
        assertThat(xMm).isWithin(TOLERANCE).of(10f)
        assertThat(yMm).isWithin(TOLERANCE).of(5f)
    }

    @Test
    fun `px to mm round trips back to the original value`() {
        val original = 733.5f

        val restored = metrics.toPxX(metrics.toMmX(original))

        assertThat(restored).isWithin(TOLERANCE).of(original)
    }

    @Test
    fun `normalizes coordinates to the zero to one range`() {
        assertThat(metrics.toNormalizedX(500f)).isWithin(TOLERANCE).of(0.5f)
        assertThat(metrics.toNormalizedY(2000f)).isWithin(TOLERANCE).of(1f)
    }

    @Test
    fun `falls back to raw px when the device reports no usable dpi`() {
        // Arrange — xdpi/ydpi 를 0 으로 채워 보내는 기기가 실제로 있다.
        val untrusted = metrics.copy(xDpi = 0f, yDpi = 0f)

        // Act & Assert — 0 으로 나눠 무한대를 내놓는 대신 px 를 그대로 돌려준다.
        assertThat(untrusted.hasPhysicalDpi).isFalse()
        assertThat(untrusted.toMmX(123f)).isEqualTo(123f)
        assertThat(untrusted.toMmY(123f)).isEqualTo(123f)
    }

    @Test
    fun `rejects a non positive screen size`() {
        val error = runCatching { ScreenMetrics(0, 100, 300f, 300f, 2f) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    private companion object {
        const val TOLERANCE = 0.001f
    }
}

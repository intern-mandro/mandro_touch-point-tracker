package com.mandro.touchtracker.ui.capture

import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import com.mandro.touchtracker.core.geometry.ScreenMetrics
import org.junit.Test

/**
 * 모눈종이 사영이 화면 비율을 지키는지 본다. 여기가 틀어지면 점들이 흩어진 모양이
 * 축 방향으로 늘어나 보여서, 눈으로 읽는 정확도 판단 자체가 틀린다.
 */
class ScreenProjectionTest {

    private val portraitScreen = ScreenMetrics(
        widthPx = 1000,
        heightPx = 2000,
        xDpi = 254f,
        yDpi = 254f,
        density = 2.5f,
    )

    @Test
    fun `fits to the limiting axis so aspect ratio is preserved`() {
        // Arrange — 캔버스가 화면보다 가로로 넉넉한 경우. 세로가 제약이 된다.
        val canvas = Size(width = 800f, height = 1000f)

        // Act
        val projection = ScreenProjection.fit(portraitScreen, canvas)

        // Assert — 2000px 를 1000f 에 맞추므로 0.5 배.
        assertThat(projection.scale).isWithin(TOLERANCE).of(0.5f)
        assertThat(projection.projectedWidth).isWithin(TOLERANCE).of(500f)
        assertThat(projection.projectedHeight).isWithin(TOLERANCE).of(1000f)
    }

    @Test
    fun `centers the projected screen inside the canvas`() {
        val canvas = Size(width = 800f, height = 1000f)

        val projection = ScreenProjection.fit(portraitScreen, canvas)

        // 남는 가로 300f 가 좌우로 반씩 나뉜다 (letterbox).
        assertThat(projection.originX).isWithin(TOLERANCE).of(150f)
        assertThat(projection.originY).isWithin(TOLERANCE).of(0f)
    }

    @Test
    fun `maps screen corners onto the projected rectangle corners`() {
        val canvas = Size(width = 800f, height = 1000f)
        val projection = ScreenProjection.fit(portraitScreen, canvas)

        val topLeft = projection.toCanvas(0f, 0f)
        val bottomRight = projection.toCanvas(1000f, 2000f)

        assertThat(topLeft.x).isWithin(TOLERANCE).of(150f)
        assertThat(topLeft.y).isWithin(TOLERANCE).of(0f)
        assertThat(bottomRight.x).isWithin(TOLERANCE).of(650f)
        assertThat(bottomRight.y).isWithin(TOLERANCE).of(1000f)
    }

    @Test
    fun `converts a millimetre grid step into canvas pixels`() {
        val canvas = Size(width = 800f, height = 1000f)
        val projection = ScreenProjection.fit(portraitScreen, canvas)

        // 254 dpi 에서 1 mm = 10 px, 0.5 배로 줄었으므로 캔버스에서는 5f.
        assertThat(projection.mmToCanvas(1f)).isWithin(TOLERANCE).of(5f)
    }

    private companion object {
        const val TOLERANCE = 0.001f
    }
}

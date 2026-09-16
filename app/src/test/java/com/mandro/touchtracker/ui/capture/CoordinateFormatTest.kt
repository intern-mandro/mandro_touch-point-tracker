package com.mandro.touchtracker.ui.capture

import com.google.common.truth.Truth.assertThat
import com.mandro.touchtracker.core.geometry.ScreenMetrics
import com.mandro.touchtracker.model.CoordinateUnit
import org.junit.Test

class CoordinateFormatTest {

    private val metrics = ScreenMetrics(
        widthPx = 1080,
        heightPx = 2400,
        xDpi = 254f, // 10 px = 1 mm -> 108 mm
        yDpi = 254f, // 240 mm
        density = 2.625f,
    )

    @Test
    fun `referenceLabel shows screen width and height in px when unit is PX`() {
        val label = CoordinateFormat.referenceLabel(CoordinateUnit.PX, metrics)
        assertThat(label).isEqualTo("1080 × 2400 px")
    }

    @Test
    fun `referenceLabel shows screen width and height in mm when unit is MM`() {
        val label = CoordinateFormat.referenceLabel(CoordinateUnit.MM, metrics)
        assertThat(label).isEqualTo("108 × 240 mm")
    }

    @Test
    fun `referenceLabel shows normalized range when unit is NORMALIZED`() {
        val label = CoordinateFormat.referenceLabel(CoordinateUnit.NORMALIZED, metrics)
        assertThat(label).isEqualTo("0.00 ~ 1.00")
    }

    @Test
    fun `referenceLabel falls back gracefully when dpi is untrusted`() {
        val untrusted = metrics.copy(xDpi = 0f, yDpi = 0f)
        val label = CoordinateFormat.referenceLabel(CoordinateUnit.MM, untrusted)
        assertThat(label).isEqualTo("1080 × 2400 px (DPI 미지원)")
    }
}

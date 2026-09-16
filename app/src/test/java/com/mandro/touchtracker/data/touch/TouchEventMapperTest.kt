package com.mandro.touchtracker.data.touch

import android.view.InputDevice
import android.view.MotionEvent
import com.google.common.truth.Truth.assertThat
import com.mandro.touchtracker.model.TouchPhase
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * MotionEvent 해석은 이 앱의 측정 정확도 그 자체다. 특히 묶음 전달(historySize)을
 * 빠뜨리면 조용히 샘플이 깎이므로 — 눈에 보이는 버그가 아니다 — 여기서 잡는다.
 */
@RunWith(RobolectricTestRunner::class)
class TouchEventMapperTest {

    private val obtained = mutableListOf<MotionEvent>()

    @After
    fun tearDown() {
        obtained.forEach { it.recycle() }
        obtained.clear()
    }

    @Test
    fun `maps action down to a single DOWN sample`() {
        // Arrange
        val event = touchEvent(MotionEvent.ACTION_DOWN, x = 120f, y = 340f)

        // Act
        val samples = TouchEventMapper.map(event, ignoreSynthetic = false)

        // Assert
        assertThat(samples).hasSize(1)
        assertThat(samples.single().phase).isEqualTo(TouchPhase.DOWN)
        assertThat(samples.single().xPx).isEqualTo(120f)
        assertThat(samples.single().yPx).isEqualTo(340f)
    }

    @Test
    fun `expands batched historical samples ahead of the current one`() {
        // Arrange — 실기기에서 ACTION_MOVE 는 과거 샘플을 함께 싣고 온다.
        val event = touchEvent(MotionEvent.ACTION_MOVE, x = 10f, y = 10f)
        event.addBatch(/* eventTime = */ 120L, /* x = */ 20f, /* y = */ 20f, 1f, 1f, 0)
        event.addBatch(/* eventTime = */ 130L, /* x = */ 30f, /* y = */ 30f, 1f, 1f, 0)

        // Act
        val samples = TouchEventMapper.map(event, ignoreSynthetic = false)

        // Assert — 과거 2개 + 현재 1개, 시간 순서대로.
        assertThat(samples).hasSize(3)
        assertThat(samples.map { it.xPx }).containsExactly(10f, 20f, 30f).inOrder()
        assertThat(samples.map { it.eventTimeUptimeMs }).isInOrder()
        assertThat(samples.all { it.phase == TouchPhase.MOVE }).isTrue()
    }

    @Test
    fun `ignores events that are not touch contact`() {
        val event = touchEvent(MotionEvent.ACTION_HOVER_MOVE, x = 1f, y = 1f)

        val samples = TouchEventMapper.map(event, ignoreSynthetic = false)

        assertThat(samples).isEmpty()
    }

    @Test
    fun `drops injected events when synthetic input is filtered out`() {
        // Arrange — adb 나 접근성 서비스가 주입한 이벤트는 실제 디바이스가 없다.
        val injected = touchEvent(MotionEvent.ACTION_DOWN, x = 5f, y = 5f, deviceId = 0)

        // Act
        val filtered = TouchEventMapper.map(injected, ignoreSynthetic = true)
        val unfiltered = TouchEventMapper.map(injected, ignoreSynthetic = false)

        // Assert — 필터를 끄면 그대로 통과한다. 버리는 건 정책이지 파싱 실패가 아니다.
        assertThat(filtered).isEmpty()
        assertThat(unfiltered).hasSize(1)
    }

    @Test
    fun `accepts events that come from a real touchscreen`() {
        val real = touchEvent(MotionEvent.ACTION_DOWN, x = 5f, y = 5f, deviceId = 4)

        assertThat(TouchEventMapper.isPhysicalTouch(real)).isTrue()
    }

    private fun touchEvent(
        action: Int,
        x: Float,
        y: Float,
        deviceId: Int = 4,
        eventTime: Long = 110L,
    ): MotionEvent {
        val properties = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
        val coords = MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            pressure = 1f
            size = 1f
        }
        return MotionEvent.obtain(
            /* downTime = */ 100L,
            /* eventTime = */ eventTime,
            /* action = */ action,
            /* pointerCount = */ 1,
            /* pointerProperties = */ arrayOf(properties),
            /* pointerCoords = */ arrayOf(coords),
            /* metaState = */ 0,
            /* buttonState = */ 0,
            /* xPrecision = */ 1f,
            /* yPrecision = */ 1f,
            /* deviceId = */ deviceId,
            /* edgeFlags = */ 0,
            /* source = */ InputDevice.SOURCE_TOUCHSCREEN,
            /* flags = */ 0,
        ).also { obtained += it }
    }
}

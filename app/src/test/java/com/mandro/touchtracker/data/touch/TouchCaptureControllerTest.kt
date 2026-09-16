package com.mandro.touchtracker.data.touch

import android.content.Context
import android.graphics.RectF
import android.view.InputDevice
import android.view.MotionEvent
import com.google.common.truth.Truth.assertThat
import com.mandro.touchtracker.core.time.SystemClock
import com.mandro.touchtracker.data.local.SettingsRepository
import com.mandro.touchtracker.data.repository.TouchSessionRepository
import com.mandro.touchtracker.model.CaptureSettings
import com.mandro.touchtracker.model.DeviceProfile
import com.mandro.touchtracker.model.TouchPoint
import com.mandro.touchtracker.model.TouchSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TouchCaptureControllerTest {

    private val obtained = mutableListOf<MotionEvent>()
    private lateinit var controller: TouchCaptureController

    private val fakeRepository = object : TouchSessionRepository {
        val appendedPoints = mutableListOf<TouchPoint>()
        val deletedSessionIds = mutableListOf<Long>()
        var sessionEnded = false
        override fun observeSessions(): Flow<List<TouchSession>> = flowOf(emptyList())
        override fun observeSession(sessionId: Long): Flow<TouchSession?> = flowOf(null)
        override fun observePoints(sessionId: Long): Flow<List<TouchPoint>> = flowOf(emptyList())
        override suspend fun getSession(sessionId: Long): TouchSession? = TouchSession(
            id = sessionId,
            name = "Test Session",
            note = "",
            startedAtEpochMs = 1000L,
            endedAtEpochMs = if (sessionEnded) 2000L else null,
            device = DeviceProfile.PREVIEW,
            pointCount = appendedPoints.size,
        )
        override suspend fun startSession(name: String, note: String, device: DeviceProfile): Long = 1L
        override suspend fun endSession(sessionId: Long) { sessionEnded = true }
        override suspend fun reopenSession(sessionId: Long) { sessionEnded = false }
        override suspend fun getPoints(sessionId: Long): List<TouchPoint> = appendedPoints.toList()
        override suspend fun appendPoints(points: List<TouchPoint>) {
            appendedPoints.addAll(points)
        }
        override suspend fun renameSession(sessionId: Long, name: String, note: String) {}
        override suspend fun deleteSession(sessionId: Long) {
            deletedSessionIds += sessionId
            appendedPoints.removeAll { it.sessionId == sessionId }   // 실제 DB 의 CASCADE 흉내
        }
    }

    private val fakeSettingsRepo = object : SettingsRepository {
        override val settings: Flow<CaptureSettings> = flowOf(CaptureSettings.DEFAULT)
        override suspend fun update(transform: (CaptureSettings) -> CaptureSettings) {}
    }

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        controller = TouchCaptureController(
            context = context,
            repository = fakeRepository,
            settingsRepository = fakeSettingsRepo,
            clock = SystemClock(),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        controller.isCaptureEnabled = true
        controller.captureBoundsInWindow = RectF(0f, 0f, 1080f, 2400f)
    }

    @After
    fun tearDown() {
        obtained.forEach { it.recycle() }
        obtained.clear()
    }

    @Test
    fun `stationary tap is recorded on action up`() {
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_DOWN, x = 100f, y = 200f))
        assertThat(controller.livePoints.value).isEmpty()

        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_UP, x = 102f, y = 201f))

        assertThat(controller.livePoints.value).hasSize(1)
        val point = controller.livePoints.value.single()
        assertThat(point.xPx).isEqualTo(100f)
        assertThat(point.yPx).isEqualTo(200f)
    }

    @Test
    fun `sliding touch is discarded and not recorded`() {
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_DOWN, x = 100f, y = 200f))
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_MOVE, x = 200f, y = 350f))
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_UP, x = 250f, y = 400f))

        assertThat(controller.livePoints.value).isEmpty()
    }

    @Test
    fun `canceled touch gesture is discarded and not recorded`() {
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_DOWN, x = 100f, y = 200f))
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_CANCEL, x = 100f, y = 200f))

        assertThat(controller.livePoints.value).isEmpty()
    }

    @Test
    fun `touch coordinates are relative to canvas bounds top left`() {
        controller.captureBoundsInWindow = RectF(60f, 320f, 1080f, 2400f)

        // Touch at window position (160, 520), which is (100, 200) on the canvas
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_DOWN, x = 160f, y = 520f))
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_UP, x = 161f, y = 521f))

        assertThat(controller.livePoints.value).hasSize(1)
        val point = controller.livePoints.value.single()
        assertThat(point.xPx).isEqualTo(100f)
        assertThat(point.yPx).isEqualTo(200f)
    }

    @Test
    fun `touching without pressing record still opens a session and saves the point`() = runTest {
        // 녹화 버튼을 누르지 않아도 첫 터치에 세션이 자동으로 열려야 한다.
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_DOWN, x = 100f, y = 200f))
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_UP, x = 101f, y = 200f))
        assertThat(controller.livePoints.value).hasSize(1)

        controller.stopSession()

        assertThat(controller.livePoints.value).isEmpty()
        assertThat(controller.activeSession.value).isNull()
        assertThat(fakeRepository.appendedPoints).hasSize(1)
        assertThat(fakeRepository.appendedPoints.single().sessionId).isEqualTo(1L)
    }

    @Test
    fun `resumeSession restores previous points and continues recording`() = runTest {
        // First capture a point and save
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_DOWN, x = 100f, y = 200f))
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_UP, x = 101f, y = 200f))
        controller.stopSession()
        val sessionId = fakeRepository.appendedPoints.first().sessionId

        // Resume the session
        val resumed = controller.resumeSession(sessionId)
        assertThat(resumed).isTrue()
        assertThat(controller.activeSession.value).isNotNull()
        assertThat(controller.activeSession.value?.id).isEqualTo(sessionId)
        assertThat(controller.livePoints.value).hasSize(1)
        assertThat(controller.livePoints.value.single().xPx).isEqualTo(100f)

        // Capture a new point in the resumed session
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_DOWN, x = 300f, y = 400f, eventTime = 200L))
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_UP, x = 301f, y = 400f, eventTime = 210L))

        assertThat(controller.livePoints.value).hasSize(2)
        val newPoint = controller.livePoints.value.last()
        assertThat(newPoint.sequence).isEqualTo(1)
        assertThat(newPoint.xPx).isEqualTo(300f)
        assertThat(newPoint.yPx).isEqualTo(400f)
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
            100L,
            eventTime,
            action,
            1,
            arrayOf(properties),
            arrayOf(coords),
            0,
            0,
            1f,
            1f,
            deviceId,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0,
        ).also { obtained += it }
    }

    @Test
    fun `discarding a session deletes it instead of leaving it half recorded`() = runTest {
        // Arrange — 기록 중에는 점이 실시간으로 DB 에 들어간다.
        controller.startSession("버릴 세션", "", DeviceProfile.PREVIEW)
        controller.isCaptureEnabled = true
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_DOWN, x = 30f, y = 40f))

        // Act
        controller.discardSession()

        // Assert — 세션도 그 점도 남으면 안 된다.
        assertThat(fakeRepository.deletedSessionIds).contains(1L)
        assertThat(fakeRepository.appendedPoints).isEmpty()
        assertThat(controller.activeSession.value).isNull()
        assertThat(controller.livePoints.value).isEmpty()
    }

    @Test
    fun `discarding without an open session does nothing`() = runTest {
        // 확인 다이얼로그가 두 번 눌리거나 이미 멈춘 뒤에 불려도 안전해야 한다.
        controller.discardSession()

        assertThat(fakeRepository.deletedSessionIds).isEmpty()
    }


    @Test
    fun `saves the same number of points whether or not record was pressed first`() = runTest {
        // 한때 경로가 둘이었다. 녹화를 켜면 터치마다 DB 로 흘러 무제한이었고,
        // 켜지 않으면 화면 버퍼(9)만 저장돼 앞쪽 점이 조용히 사라졌다.
        // 이제 두 경우가 같은 저장 루프를 타므로 개수가 같아야 한다.
        val tapCount = CaptureSettings.RECENT_POINT_LIMIT * 3
        controller.isCaptureEnabled = true

        repeat(tapCount) { index -> tapAt(x = 20f + index, y = 30f + index) }
        controller.stopSession()
        val withoutPressingRecord = fakeRepository.appendedPoints.size

        fakeRepository.appendedPoints.clear()
        controller.startSession("명시적 시작", "", DeviceProfile.PREVIEW)
        repeat(tapCount) { index -> tapAt(x = 20f + index, y = 30f + index) }
        controller.stopSession()
        val afterPressingRecord = fakeRepository.appendedPoints.size

        assertThat(withoutPressingRecord).isEqualTo(tapCount)
        assertThat(afterPressingRecord).isEqualTo(tapCount)
    }

    @Test
    fun `keeps only the newest points once the live buffer is full`() = runTest {
        // 버퍼는 FIFO — 가장 오래된 것부터 밀려난다. 기록 중이면 이미 DB 에 들어간
        // 뒤라 안전하지만, 상한 자체는 지켜져야 메모리가 무한히 자라지 않는다.
        controller.isCaptureEnabled = true
        val limit = CaptureSettings.DEFAULT_LIVE_BUFFER_SIZE

        repeat(limit + 5) { index -> tapAt(x = 20f + index, y = 30f) }

        assertThat(controller.livePoints.value).hasSize(limit)
        // 앞의 5 개가 밀려났으므로 가장 오래된 점은 6 번째 탭이다.
        assertThat(controller.livePoints.value.first().xPx).isEqualTo(25f)
    }

    /** 같은 자리에서 눌렀다 떼는 유효한 단일 탭 한 번. */
    private fun tapAt(x: Float, y: Float) {
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_DOWN, x = x, y = y))
        controller.onMotionEvent(touchEvent(MotionEvent.ACTION_UP, x = x, y = y))
    }


    @Test
    fun `saving a session wipes the canvas so the next run starts empty`() = runTest {
        // Arrange
        controller.isCaptureEnabled = true
        tapAt(x = 100f, y = 200f)
        tapAt(x = 140f, y = 260f)
        assertThat(controller.livePoints.value).hasSize(2)

        // Act
        controller.stopSession()

        // Assert — 확정된 측정은 세션에만 남고 화면에는 남지 않는다.
        assertThat(controller.livePoints.value).isEmpty()
        assertThat(fakeRepository.appendedPoints).hasSize(2)
    }

    @Test
    fun `points measured after saving do not mix with the previous session`() = runTest {
        controller.isCaptureEnabled = true
        tapAt(x = 100f, y = 200f)
        controller.stopSession()

        tapAt(x = 300f, y = 400f)

        // 화면에는 새 회차의 점 하나만 보여야 한다.
        assertThat(controller.livePoints.value).hasSize(1)
        assertThat(controller.livePoints.value.single().xPx).isEqualTo(300f)
        // 세션 안에서의 정렬 키도 0 부터 다시 시작한다.
        assertThat(controller.livePoints.value.single().sequence).isEqualTo(0)
    }


    @Test
    fun `touching without the record button does not count as explicit recording`() = runTest {
        // 자동으로 열린 세션은 저장은 되지만 "녹화 중" 이 아니다 —
        // 상단바는 이 값으로 표시등을 띄울지 초기화 버튼을 둘지 가른다.
        controller.isCaptureEnabled = true

        tapAt(x = 100f, y = 200f)

        assertThat(controller.activeSession.value).isNotNull()
        assertThat(controller.isExplicitRecording.value).isFalse()
    }

    @Test
    fun `pressing record marks the session as explicit until it ends`() = runTest {
        controller.startSession("직접 시작", "", DeviceProfile.PREVIEW)
        assertThat(controller.isExplicitRecording.value).isTrue()

        controller.stopSession()

        assertThat(controller.isExplicitRecording.value).isFalse()
    }

    @Test
    fun `discarding also clears the explicit recording flag`() = runTest {
        controller.startSession("직접 시작", "", DeviceProfile.PREVIEW)

        controller.discardSession()

        assertThat(controller.isExplicitRecording.value).isFalse()
    }

    @Test
    fun `both save paths persist through the same session logic`() = runTest {
        // 초기화 확인창의 "저장" 과 녹화 정지의 "저장" 은 같은 stopSession 을 탄다.
        // 저장되는 점 개수가 경로에 따라 달라지면 안 된다.
        controller.isCaptureEnabled = true
        repeat(3) { index -> tapAt(x = 100f + index, y = 200f) }
        controller.stopSession()
        val viaAutoSession = fakeRepository.appendedPoints.size

        fakeRepository.appendedPoints.clear()
        controller.startSession("직접 시작", "", DeviceProfile.PREVIEW)
        repeat(3) { index -> tapAt(x = 100f + index, y = 200f) }
        controller.stopSession()
        val viaExplicitRecording = fakeRepository.appendedPoints.size

        assertThat(viaAutoSession).isEqualTo(3)
        assertThat(viaExplicitRecording).isEqualTo(3)
    }

}

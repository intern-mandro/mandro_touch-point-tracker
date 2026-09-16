package com.mandro.touchtracker.data.touch

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.mandro.touchtracker.core.time.Clock
import com.mandro.touchtracker.di.ApplicationScope
import com.mandro.touchtracker.model.CaptureSettings
import com.mandro.touchtracker.model.DeviceProfile
import com.mandro.touchtracker.model.TouchPhase
import com.mandro.touchtracker.model.TouchPoint
import com.mandro.touchtracker.model.TouchSession
import com.mandro.touchtracker.data.local.SettingsRepository
import com.mandro.touchtracker.data.repository.TouchSessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot
@Singleton
class TouchCaptureController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TouchSessionRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val _activeSession = MutableStateFlow<TouchSession?>(null)

    // null 인 경우 화면은 기록되지 않음 (무시함)
    val activeSession: StateFlow<TouchSession?> = _activeSession.asStateFlow()

    private val _isExplicitRecording = MutableStateFlow(false)

    /**
     * 사람이 녹화 버튼으로 시작한 세션인가.
     *
     * 세션은 두 가지 경로로 열린다 — 녹화 버튼을 누르거나, 그냥 화면을 눌러서
     * 자동으로. 저장되는 내용은 똑같지만 화면에 보여 줄 것은 다르다.
     * "녹화 중" 표시등은 사람이 직접 켰을 때만 뜬다.
     */
    val isExplicitRecording: StateFlow<Boolean> = _isExplicitRecording.asStateFlow()

    private val _livePoints = MutableStateFlow<List<TouchPoint>>(emptyList())

    /** 방금 들어온 순서대로 쌓인 최근 점들. 오래된 것부터. 렌더링용. */
    val livePoints: StateFlow<List<TouchPoint>> = _livePoints.asStateFlow()

    /**
     * 저장 대기열. 점과 "여기까지 비워라" 신호가 같은 줄에 선다 — 순서가 보장되므로
     * flush 신호가 완료되면 그 앞의 점은 전부 저장이 끝난 상태다.
     */
    private val pending = Channel<PendingItem>(Channel.UNLIMITED)

    private sealed interface PendingItem {
        @JvmInline value class Point(val point: TouchPoint) : PendingItem
        class Flush(val ack: CompletableDeferred<Unit>) : PendingItem
    }

    @Volatile
    private var settings: CaptureSettings = CaptureSettings.DEFAULT

    /** 모눈종이 탭이 화면에 떠 있을 때만 true 가 된다. 데이터 탭 등에서는 터치가 기록되지 않는다. */
    @Volatile
    var isCaptureEnabled: Boolean = false

    /** 모눈종이 캔버스의 윈도우 내 영역. 상단 탑바나 탭 버튼 터치 등을 배제한다. */
    @Volatile
    var captureBoundsInWindow: android.graphics.RectF? = null

    /**
     * 자동 세션을 열 때 쓸 좌표계. 화면이 실제 캔버스 크기를 잰 뒤 넣어 준다.
     *
     * Activity 를 직접 들고 있으면 싱글턴이 화면을 붙잡아 새므로, 값 타입만 받는다.
     */
    @Volatile
    var sessionDeviceProfile: DeviceProfile = DeviceProfile.PREVIEW

    /** 터치 디스패치 스레드에서만 건드린다 — 동기화 불필요. */
    private var sequence = 0

    private var sessionStartUptimeMs = 0L
    private var sessionElapsedOffsetMs = 0L
    private var firstLiveTouchUptimeMs = 0L

    /** 슬라이드 제스처(스와이프/스크롤/드래그) 판정 임계치 (기기 scaledTouchSlop 기반, 최소 24px) */
    private val slideThresholdPx: Float by lazy {
        runCatching {
            ViewConfiguration.get(context).scaledTouchSlop.toFloat()
        }.getOrDefault(24f).coerceAtLeast(24f)
    }

    /** 슬라이드 제스처 판별을 위한 진행 중 터치 임시 보관 */
    private data class PendingTouch(
        val downSample: TouchSample,
        var maxDistancePx: Float = 0f,
        var hasSlid: Boolean = false,
    )

    private val pendingTouches = mutableMapOf<Int, PendingTouch>()

    init {
        scope.launch { settingsRepository.settings.collect { settings = it } }
        scope.launch { drainPending() }
    }

    // 새 세션 열고 기록하기
    suspend fun startSession(name: String, note: String, device: DeviceProfile) {
        stopSession()

        val startedAt = clock.epochMs()
        val id = repository.startSession(name, note, device)
        _isExplicitRecording.value = true
        sequence = 0
        sessionStartUptimeMs = clock.uptimeMs()
        sessionElapsedOffsetMs = 0L
        firstLiveTouchUptimeMs = 0L
        pendingTouches.clear()
        _livePoints.value = emptyList()
        _activeSession.value = TouchSession(
            id = id,
            name = name,
            note = note,
            startedAtEpochMs = startedAt,
            device = device,
        )
    }

    /**
     * 기록을 끝내고 화면을 비운다. 열린 세션이 없으면 아무것도 하지 않는다.
     *
     * 화면을 같이 비우는 게 핵심이다. 확정된 측정은 **세션 데이터로만** 남아야 한다 —
     * 캔버스에 남겨 두면 다음 회차의 점과 섞여서, 보이는 점이 어느 세션 것인지
     * 구분할 수 없게 된다.
     */
    suspend fun stopSession() {
        val session = _activeSession.value ?: return
        _activeSession.value = null
        _isExplicitRecording.value = false
        firstLiveTouchUptimeMs = 0L
        pendingTouches.clear()
        flushPending()
        repository.endSession(session.id)
        clearLivePoints()
    }

    /**
     * 기록 중인 세션을 통째로 버린다.
     *
     * 기록 중에는 점이 실시간으로 DB 에 들어가므로, "저장 안 함" 은 곧 삭제다.
     * 대기열을 먼저 비우는 순서가 중요하다 — 안 그러면 삭제 뒤에 남은 점이
     * 없어진 세션을 참조하며 되살아난다.
     *
     * 되돌릴 수 없다. 호출 전에 반드시 사용자 확인을 받는다.
     */
    suspend fun discardSession() {
        val session = _activeSession.value ?: return
        _activeSession.value = null
        _isExplicitRecording.value = false
        firstLiveTouchUptimeMs = 0L
        pendingTouches.clear()
        flushPending()
        repository.deleteSession(session.id)
        clearLivePoints()
    }

    // 화면에 그려지는 점만 지움 (저장된 데이터는 건들이지 않음)
    fun clearLivePoints() {
        pendingTouches.clear()
        _livePoints.value = emptyList()
        // sequence 는 세션 안에서의 정렬 키다. 열린 세션이 있는데 0 으로 되돌리면
        // 이미 저장된 점과 번호가 겹쳐 내보내기 순서가 뒤섞인다.
        if (_activeSession.value == null) {
            sequence = 0
            firstLiveTouchUptimeMs = 0L
        }
    }

    /** 기존 세션의 마지막 상태를 복원하고 이어서 기록을 재개 */
    suspend fun resumeSession(sessionId: Long): Boolean {
        stopSession()

        val session = repository.getSession(sessionId) ?: return false
        repository.reopenSession(sessionId)

        val allPoints = repository.getPoints(sessionId)
        val maxSeq = allPoints.maxOfOrNull { it.sequence } ?: -1
        sequence = maxSeq + 1

        val bufferSize = settings.liveBufferSize
        val recentPoints = allPoints.takeLast(bufferSize)
        _livePoints.value = recentPoints

        sessionElapsedOffsetMs = allPoints.lastOrNull()?.elapsedMs ?: 0L
        sessionStartUptimeMs = clock.uptimeMs()
        firstLiveTouchUptimeMs = 0L
        pendingTouches.clear()

        _activeSession.value = session.copy(endedAtEpochMs = null)
        // 이어서 기록하기는 사람이 고른 것이므로 명시적 녹화로 친다.
        _isExplicitRecording.value = true
        return true
    }

    fun onMotionEvent(event: MotionEvent) {
        if (!isCaptureEnabled) {
            pendingTouches.clear()
            return
        }

        val bounds = captureBoundsInWindow
        val current = settings

        val samples = TouchEventMapper.map(event, ignoreSynthetic = current.ignoreSyntheticInput)
        if (samples.isEmpty()) return

        val offsetX = bounds?.left ?: 0f
        val offsetY = bounds?.top ?: 0f

        // MOVE 이벤트를 전부 추적하는 모드인 경우 (원시 궤적 모드)
        if (current.recordMoveEvents) {
            recordContinuousSamples(samples, current, offsetX, offsetY)
            return
        }

        // 단일 좌표 측정 모드: 슬라이드(드래그) 판정 적용
        val slopPx = slideThresholdPx

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                val x = event.getX(index)
                val y = event.getY(index)
                if (bounds == null || bounds.contains(x, y)) {
                    val pointerId = event.getPointerId(index)
                    val rawSample = samples.firstOrNull { it.pointerId == pointerId }
                    if (rawSample != null) {
                        val canvasSample = rawSample.copy(
                            xPx = rawSample.xPx - offsetX,
                            yPx = rawSample.yPx - offsetY,
                        )
                        pendingTouches[pointerId] = PendingTouch(canvasSample)
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(i)
                    val pending = pendingTouches[pointerId] ?: continue
                    val currentX = event.getX(i) - offsetX
                    val currentY = event.getY(i) - offsetY
                    val dx = currentX - pending.downSample.xPx
                    val dy = currentY - pending.downSample.yPx
                    val dist = hypot(dx, dy)
                    if (dist > pending.maxDistancePx) {
                        pending.maxDistancePx = dist
                    }
                    if (dist > slopPx) {
                        // 기준 거리 이상 이동 시 슬라이드로 판정하여 기록 제외 플래그 설정
                        pending.hasSlid = true
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val index = event.actionIndex
                val pointerId = event.getPointerId(index)
                val pending = pendingTouches.remove(pointerId)
                if (pending != null && !pending.hasSlid) {
                    val currentX = event.getX(index) - offsetX
                    val currentY = event.getY(index) - offsetY
                    val dx = currentX - pending.downSample.xPx
                    val dy = currentY - pending.downSample.yPx
                    if (hypot(dx, dy) <= slopPx) {
                        recordSinglePoint(pending.downSample, current)
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                val index = event.actionIndex
                val pointerId = event.getPointerId(index)
                val pending = pendingTouches.remove(pointerId)
                if (pending != null && !pending.hasSlid) {
                    val currentX = event.getX(index) - offsetX
                    val currentY = event.getY(index) - offsetY
                    val dx = currentX - pending.downSample.xPx
                    val dy = currentY - pending.downSample.yPx
                    if (hypot(dx, dy) <= slopPx) {
                        recordSinglePoint(pending.downSample, current)
                    }
                }
                pendingTouches.clear()
            }

            MotionEvent.ACTION_CANCEL -> {
                // 부모 뷰(페이저, 스크롤 등)가 제스처를 가로챈 경우 터치 무효화
                pendingTouches.clear()
            }
        }
    }

    /** 슬라이드가 아닌 유효한 단일 탭을 측정 점으로 등록 */
    private fun recordSinglePoint(sample: TouchSample, current: CaptureSettings) {
        val epochNow = clock.epochMs()
        val uptimeNow = clock.uptimeMs()
        val session = _activeSession.value
        val referenceUptimeMs = if (session != null) {
            sessionStartUptimeMs
        } else {
            if (firstLiveTouchUptimeMs == 0L || _livePoints.value.isEmpty()) {
                firstLiveTouchUptimeMs = sample.eventTimeUptimeMs
            }
            firstLiveTouchUptimeMs
        }

        val baseElapsedMs = (sample.eventTimeUptimeMs - referenceUptimeMs).coerceAtLeast(0L)
        val elapsedMs = if (session != null) baseElapsedMs + sessionElapsedOffsetMs else baseElapsedMs
        val point = TouchPoint(
            sessionId = session?.id ?: TouchSession.NO_ID,
            sequence = sequence++,
            pointerId = sample.pointerId,
            phase = TouchPhase.DOWN,
            xPx = sample.xPx,
            yPx = sample.yPx,
            pressure = sample.pressure,
            touchMajorPx = sample.touchMajorPx,
            touchMinorPx = sample.touchMinorPx,
            orientationRad = sample.orientationRad,
            elapsedMs = elapsedMs,
            epochMs = epochNow - (uptimeNow - sample.eventTimeUptimeMs),
        )

        _livePoints.value = (_livePoints.value + point).takeLast(current.liveBufferSize)
        // 세션이 열려 있든 아니든 똑같이 대기열로 보낸다. 세션이 없으면 저장 루프가
        // 하나 만들어 붙인다 — 저장 경로가 하나뿐이라 경로마다 개수가 달라질 수 없다.
        pending.trySend(PendingItem.Point(point))
    }

    /** recordMoveEvents 가 켜진 특수 모드용 연속 샘플 등록 */
    private fun recordContinuousSamples(
        samples: List<TouchSample>,
        current: CaptureSettings,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
    ) {
        val epochNow = clock.epochMs()
        val uptimeNow = clock.uptimeMs()
        val accepted = ArrayList<TouchPoint>(samples.size)
        val session = _activeSession.value
        val referenceUptimeMs = if (session != null) {
            sessionStartUptimeMs
        } else {
            if (firstLiveTouchUptimeMs == 0L || _livePoints.value.isEmpty()) {
                firstLiveTouchUptimeMs = samples.first().eventTimeUptimeMs
            }
            firstLiveTouchUptimeMs
        }

        for (sample in samples) {
            val baseElapsedMs = (sample.eventTimeUptimeMs - referenceUptimeMs).coerceAtLeast(0L)
            val elapsedMs = if (session != null) baseElapsedMs + sessionElapsedOffsetMs else baseElapsedMs
            accepted += TouchPoint(
                sessionId = session?.id ?: TouchSession.NO_ID,
                sequence = sequence++,
                pointerId = sample.pointerId,
                phase = sample.phase,
                xPx = sample.xPx - offsetX,
                yPx = sample.yPx - offsetY,
                pressure = sample.pressure,
                touchMajorPx = sample.touchMajorPx,
                touchMinorPx = sample.touchMinorPx,
                orientationRad = sample.orientationRad,
                elapsedMs = elapsedMs,
                epochMs = epochNow - (uptimeNow - sample.eventTimeUptimeMs),
            )
        }
        if (accepted.isEmpty()) return

        _livePoints.value = (_livePoints.value + accepted).takeLast(current.liveBufferSize)
        for (point in accepted) pending.trySend(PendingItem.Point(point))
    }

    /** 대기열을 묶음으로 저장한다. 앱이 살아 있는 동안 계속 돈다. */
    private suspend fun drainPending() {
        val batch = ArrayList<TouchPoint>(MAX_BATCH_SIZE)

        suspend fun commit() {
            if (batch.isEmpty()) return
            val sessionId = ensureSessionId()
            // 세션이 없을 때 들어온 점은 sessionId 가 비어 있다. 여기서 채운다.
            repository.appendPoints(
                batch.map { if (it.sessionId == TouchSession.NO_ID) it.copy(sessionId = sessionId) else it },
            )
            batch.clear()
        }

        while (true) {
            when (val head = pending.receive()) {   // 하나 올 때까지 대기
                is PendingItem.Point -> batch += head.point
                is PendingItem.Flush -> { commit(); head.ack.complete(Unit); continue }
            }
            // 이미 와 있는 것들만 더 긁어 모은다 — 기다리지는 않는다.
            while (batch.size < MAX_BATCH_SIZE) {
                when (val next = pending.tryReceive().getOrNull()) {
                    null -> break
                    is PendingItem.Point -> batch += next.point
                    is PendingItem.Flush -> { commit(); next.ack.complete(Unit) }
                }
            }
            commit()
        }
    }

    /**
     * 저장할 세션 id 를 돌려준다. 열린 세션이 없으면 여기서 만든다.
     *
     * 사용자가 실행 버튼을 누르지 않고 그냥 화면을 눌러도 측정이 남게 하는 지점이다.
     * 저장 루프(단일 코루틴)에서만 불리므로 세션이 두 번 만들어질 수 없다.
     */
    private suspend fun ensureSessionId(): Long {
        _activeSession.value?.let { return it.id }

        val device = sessionDeviceProfile
        val name = autoSessionName()
        val startedAt = clock.epochMs()
        val id = repository.startSession(name, note = "", device = device)
        _activeSession.value = TouchSession(
            id = id,
            name = name,
            note = "",
            startedAtEpochMs = startedAt,
            device = device,
        )
        return id
    }

    private fun autoSessionName(): String =
        java.text.SimpleDateFormat(SESSION_NAME_PATTERN, java.util.Locale.US)
            .format(java.util.Date(clock.epochMs()))

    private suspend fun flushPending() {
        val ack = CompletableDeferred<Unit>()
        pending.send(PendingItem.Flush(ack))
        ack.await()
    }

    private companion object {
        const val MAX_BATCH_SIZE = 256
        const val SESSION_NAME_PATTERN = "MM/dd HH:mm:ss"
    }
}

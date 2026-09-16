package com.mandro.touchtracker.data.touch

import android.view.MotionEvent
import com.mandro.touchtracker.core.time.Clock
import com.mandro.touchtracker.di.ApplicationScope
import com.mandro.touchtracker.model.CaptureSettings
import com.mandro.touchtracker.model.DeviceProfile
import com.mandro.touchtracker.model.TouchPhase
import com.mandro.touchtracker.model.TouchPoint
import com.mandro.touchtracker.model.TouchSession
import com.mandro.touchtracker.data.local.SettingsRepository
import com.mandro.touchtracker.data.repository.TouchSessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 전체에서 하나뿐인 터치 수집기.
 *
 * ### 왜 Composable 안이 아니라 여기인가
 * 캡처는 **Activity 의 `dispatchTouchEvent` 에서** 들어온다 ([MainActivity] 참고).
 * 이유:
 * - 어떤 탭이 떠 있든 로봇이 화면을 누르면 기록돼야 한다. 캡처를 특정 화면의
 *   `pointerInput` 에 매달면 탭을 옮기는 순간 측정이 끊긴다.
 * - 관찰만 하고 이벤트를 소비하지 않으므로, 같은 터치가 그대로 Compose 로 흘러가
 *   탭 전환·버튼도 평소처럼 동작한다.
 *
 * ### 쓰기 경로
 * 점은 곧바로 DB 에 넣지 않는다. MOVE 는 초당 수백 건이라 건건이 insert 하면
 * 터치 디스패치 스레드가 막힌다. [pending] 채널에 던지고 [drainPending] 이
 * 묶음으로 저장한다. UI 는 DB 를 기다리지 않고 [livePoints] 를 즉시 본다.
 */
@Singleton
class TouchCaptureController @Inject constructor(
    private val repository: TouchSessionRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val _activeSession = MutableStateFlow<TouchSession?>(null)

    /** null 이면 기록 중이 아니다. 화면 터치는 무시된다. */
    val activeSession: StateFlow<TouchSession?> = _activeSession.asStateFlow()

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

    /** 터치 디스패치 스레드에서만 건드린다 — 동기화 불필요. */
    private var sequence = 0
    private var sessionStartUptimeMs = 0L

    init {
        scope.launch { settingsRepository.settings.collect { settings = it } }
        scope.launch { drainPending() }
    }

    /**
     * 새 세션을 열고 기록을 시작한다.
     *
     * @param device 지금 화면의 좌표계. 세션이 끝날 때까지 바뀌지 않는다고 가정한다
     *               (그래서 MainActivity 가 방향 고정이다).
     */
    suspend fun startSession(name: String, note: String, device: DeviceProfile) {
        stopSession()

        val startedAt = clock.epochMs()
        val id = repository.startSession(name, note, device)
        sequence = 0
        sessionStartUptimeMs = clock.uptimeMs()
        _livePoints.value = emptyList()
        _activeSession.value = TouchSession(
            id = id,
            name = name,
            note = note,
            startedAtEpochMs = startedAt,
            device = device,
        )
    }

    /** 기록을 끝낸다. 열린 세션이 없으면 아무 일도 하지 않는다. */
    suspend fun stopSession() {
        val session = _activeSession.value ?: return
        _activeSession.value = null
        flushPending()
        repository.endSession(session.id)
    }

    /** 화면에 그려진 점만 지운다. 저장된 데이터는 건드리지 않는다. */
    fun clearLivePoints() {
        _livePoints.value = emptyList()
    }

    /**
     * Activity 의 터치 디스패치에서 호출된다. **이벤트를 소비하지 않는다.**
     *
     * 이 함수는 UI 스레드에서 매 터치마다 도므로 할당과 작업량을 최소로 유지한다.
     */
    fun onMotionEvent(event: MotionEvent) {
        val session = _activeSession.value ?: return
        val current = settings

        val samples = TouchEventMapper.map(event, ignoreSynthetic = current.ignoreSyntheticInput)
        if (samples.isEmpty()) return

        val epochNow = clock.epochMs()
        val uptimeNow = clock.uptimeMs()
        val accepted = ArrayList<TouchPoint>(samples.size)

        for (sample in samples) {
            if (!current.recordMoveEvents && sample.phase == TouchPhase.MOVE) continue

            val elapsedMs = sample.eventTimeUptimeMs - sessionStartUptimeMs
            accepted += TouchPoint(
                sessionId = session.id,
                sequence = sequence++,
                pointerId = sample.pointerId,
                phase = sample.phase,
                xPx = sample.xPx,
                yPx = sample.yPx,
                pressure = sample.pressure,
                touchMajorPx = sample.touchMajorPx,
                touchMinorPx = sample.touchMinorPx,
                orientationRad = sample.orientationRad,
                elapsedMs = elapsedMs,
                // 샘플의 eventTime 은 단조 시계다. 절대 시각으로 되돌리려면
                // "지금의 절대 시각 - 지금의 단조 시각" 만큼 평행이동시킨다.
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
            repository.appendPoints(batch)
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
     * 대기열이 비워질 때까지 기다린다.
     *
     * 채널에서 직접 꺼내 오지 않는 게 중요하다 — [drainPending] 이 이미 꺼내 갔지만
     * 아직 저장 전인 점을 놓치기 때문이다. 대신 신호를 줄 맨 뒤에 세우고 처리되길 기다린다.
     */
    private suspend fun flushPending() {
        val ack = CompletableDeferred<Unit>()
        pending.send(PendingItem.Flush(ack))
        ack.await()
    }

    private companion object {
        const val MAX_BATCH_SIZE = 256
    }
}

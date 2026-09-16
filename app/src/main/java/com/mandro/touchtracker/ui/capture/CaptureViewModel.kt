package com.mandro.touchtracker.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mandro.touchtracker.core.geometry.ScreenMetrics
import com.mandro.touchtracker.data.touch.TouchCaptureController
import com.mandro.touchtracker.model.CoordinateUnit
import com.mandro.touchtracker.model.DeviceProfile
import com.mandro.touchtracker.data.local.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val captureController: TouchCaptureController,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val messages = MutableStateFlow<String?>(null)
    private val deviceMetrics = MutableStateFlow(ScreenMetrics.PREVIEW)

    /**
     * 세션과 "직접 켠 녹화인가" 는 항상 같이 움직인다. 한 덩어리로 묶어 두면
     * combine 인자를 늘리지 않아도 되고, 둘이 어긋난 순간을 UI 가 볼 일도 없다.
     */
    private val sessionState = combine(
        captureController.activeSession,
        captureController.isExplicitRecording,
    ) { session, explicit -> session to explicit }

    val uiState: StateFlow<CaptureUiState> = combine(
        sessionState,
        captureController.livePoints,
        settingsRepository.settings,
        messages,
        deviceMetrics,
    ) { (session, explicit), points, settings, message, metricsFallback ->
        CaptureUiState(
            isRecording = session != null,
            isExplicitRecording = explicit,
            sessionName = session?.name.orEmpty(),
            settings = settings,
            // 기록 중이면 그 세션의 좌표계가 진실이다. 아니면 기기 실제 해상도(deviceMetrics) 사용.
            metrics = session?.metrics ?: metricsFallback,
            livePoints = points,
            message = message,
        )
    }.stateIn(
        scope = viewModelScope,
        // 화면이 잠깐 가려져도(설정 화면 등) 다시 돌아왔을 때 모으던 걸 안 버리게.
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = CaptureUiState(),
    )

    /** 기기 프로파일 원본. 캔버스 크기를 잴 때마다 좌표계만 갈아끼워 다시 쓴다. */
    private var baseDeviceProfile: DeviceProfile = DeviceProfile.PREVIEW

    /** 기기 실제 화면 해상도를 뷰모델에 등록 (기록 시작 전에도 실기기 해상도 표시) */
    fun setDeviceProfile(profile: DeviceProfile) {
        baseDeviceProfile = profile
        val bounds = captureController.captureBoundsInWindow
        deviceMetrics.value = if (bounds != null && bounds.width() > 0f && bounds.height() > 0f) {
            profile.metrics.copy(
                widthPx = bounds.width().roundToInt(),
                heightPx = bounds.height().roundToInt(),
            )
        } else {
            profile.metrics
        }
        publishSessionProfile()
    }

    /**
     * 자동으로 열릴 세션이 쓸 좌표계를 컨트롤러에 넘긴다.
     *
     * 터치가 들어오는 순간 세션이 만들어지므로, 그 전에 좌표계가 준비돼 있어야
     * 세션에 엉뚱한 화면 크기가 박제된다.
     */
    private fun publishSessionProfile() {
        captureController.sessionDeviceProfile =
            baseDeviceProfile.copy(metrics = deviceMetrics.value)
    }

    /**
     * 기록을 켜고 끈다.
     *
     * @param deviceProfile 지금 화면의 좌표계. Activity 가 알고 있어서 호출 시점에 받는다.
     */
    /**
     * 녹화 버튼. 끄면 세션을 확정하고, 켜면 빈 세션을 미리 연다.
     *
     * 켜지 않아도 화면을 누르면 세션이 자동으로 열린다 — 이 버튼은 "지금부터가
     * 한 회차다" 를 명시적으로 끊어 주는 용도다.
     */
    fun toggleRecording() {
        viewModelScope.launch {
            if (captureController.activeSession.value != null) {
                captureController.stopSession()
                messages.value = "세션을 저장했습니다"
            } else {
                captureController.startSession(
                    name = defaultSessionName(),
                    note = "",
                    device = captureController.sessionDeviceProfile,
                )
                messages.value = null
            }
        }
    }

    /** 저장하지 않고 버린다. 점이 이미 DB 에 들어가 있으므로 세션 행까지 지운다. */
    fun discardCurrentSession() {
        viewModelScope.launch {
            captureController.discardSession()
            messages.value = "저장하지 않고 초기화했습니다"
        }
    }

    /**
     * 기록을 멈추고 세션을 남긴다. 화면을 떠나기 전 "저장" 을 골랐을 때.
     *
     * @param onFinished 저장이 끝난 뒤 실행할 동작(화면 이동). 저장이 DB 에 반영되기
     *        전에 이동하면 세션 목록에 아직 "기록 중" 으로 보이므로 콜백으로 받는다.
     */
    fun stopRecordingThen(onFinished: () -> Unit) {
        viewModelScope.launch {
            captureController.stopSession()
            messages.value = "세션을 저장했습니다"
            onFinished()
        }
    }

    /** 기록 중인 세션을 버리고 이동한다. DB 행까지 지우므로 되돌릴 수 없다. */
    fun discardRecordingThen(onFinished: () -> Unit) {
        viewModelScope.launch {
            captureController.discardSession()
            messages.value = "세션을 저장하지 않았습니다"
            onFinished()
        }
    }

    fun clearLivePoints() {
        captureController.clearLivePoints()
    }

    fun saveAndClearLivePoints() {
        viewModelScope.launch {
            captureController.stopSession()
            messages.value = "세션을 저장하고 초기화했습니다"
        }
    }

    fun setCaptureActive(active: Boolean) {
        captureController.isCaptureEnabled = active
    }

    fun setCaptureBounds(bounds: android.graphics.RectF?) {
        if (bounds != null) {
            captureController.captureBoundsInWindow = bounds
            if (bounds.width() > 0f && bounds.height() > 0f) {
                val w = bounds.width().roundToInt()
                val h = bounds.height().roundToInt()
                deviceMetrics.value = deviceMetrics.value.copy(
                    widthPx = w,
                    heightPx = h,
                )
                publishSessionProfile()
            }
        }
    }

    fun setCoordinateUnit(unit: CoordinateUnit) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(coordinateUnit = unit) }
        }
    }

    fun consumeMessage() {
        messages.value = null
    }

    /** 사람이 나중에 이름을 바꾼다는 전제로, 일단 시각만 박아 구분되게 한다. */
    private fun defaultSessionName(): String =
        SimpleDateFormat(SESSION_NAME_PATTERN, Locale.US).format(Date())

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        const val SESSION_NAME_PATTERN = "MM/dd HH:mm:ss"
    }
}

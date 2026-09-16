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

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val captureController: TouchCaptureController,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val messages = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CaptureUiState> = combine(
        captureController.activeSession,
        captureController.livePoints,
        settingsRepository.settings,
        messages,
    ) { session, points, settings, message ->
        CaptureUiState(
            isRecording = session != null,
            sessionName = session?.name.orEmpty(),
            settings = settings,
            // 기록 중이면 그 세션의 좌표계가 진실이다. 아니면 프리뷰 기본값으로,
            // 세션이 없을 때도 모눈종이가 뭔가는 그릴 수 있게 한다.
            metrics = session?.metrics ?: ScreenMetrics.PREVIEW,
            livePoints = points,
            message = message,
        )
    }.stateIn(
        scope = viewModelScope,
        // 화면이 잠깐 가려져도(설정 화면 등) 다시 돌아왔을 때 모으던 걸 안 버리게.
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = CaptureUiState(),
    )

    /**
     * 기록을 켜고 끈다.
     *
     * @param deviceProfile 지금 화면의 좌표계. Activity 가 알고 있어서 호출 시점에 받는다.
     */
    fun toggleRecording(deviceProfile: () -> DeviceProfile) {
        viewModelScope.launch {
            if (captureController.activeSession.value != null) {
                captureController.stopSession()
                messages.value = "세션을 저장했습니다"
            } else {
                captureController.startSession(
                    name = defaultSessionName(),
                    note = "",
                    device = deviceProfile(),
                )
                messages.value = null
            }
        }
    }

    fun clearLivePoints() {
        captureController.clearLivePoints()
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

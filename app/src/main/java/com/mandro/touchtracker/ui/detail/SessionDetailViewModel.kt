package com.mandro.touchtracker.ui.detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mandro.touchtracker.core.AppResult
import com.mandro.touchtracker.model.ExportFormat
import com.mandro.touchtracker.model.TouchPoint
import com.mandro.touchtracker.model.TouchSession
import com.mandro.touchtracker.data.export.SessionExporter
import com.mandro.touchtracker.data.repository.TouchSessionRepository
import com.mandro.touchtracker.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionDetailUiState(
    val session: TouchSession? = null,
    val points: List<TouchPoint> = emptyList(),
    val isExporting: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: TouchSessionRepository,
    private val exporter: SessionExporter,
) : ViewModel() {

    private val sessionId: Long =
        savedStateHandle[Screen.SessionDetail.ARG_SESSION_ID] ?: TouchSession.NO_ID

    private val exporting = MutableStateFlow(false)
    private val messages = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SessionDetailUiState> = combine(
        repository.observeSession(sessionId),
        repository.observePoints(sessionId),
        exporting,
        messages,
    ) { session, points, isExporting, message ->
        SessionDetailUiState(session, points, isExporting, message)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = SessionDetailUiState(),
    )

    /** 파일 만들기 다이얼로그에 넣을 기본 이름. 화면이 SAF 를 띄우기 직전에 부른다. */
    suspend fun suggestFileName(format: ExportFormat): String =
        exporter.suggestFileName(sessionId, format)

    fun export(format: ExportFormat, target: Uri) {
        viewModelScope.launch {
            exporting.value = true
            messages.value = when (val result = exporter.export(sessionId, format, target)) {
                is AppResult.Success -> "내보내기 완료 — ${result.value}"
                is AppResult.Failure -> result.message
            }
            exporting.value = false
        }
    }

    fun consumeMessage() {
        messages.value = null
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

package com.mandro.touchtracker.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mandro.touchtracker.model.TouchSession
import com.mandro.touchtracker.data.repository.TouchSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionListUiState(
    val sessions: List<TouchSession> = emptyList(),
    val isLoaded: Boolean = false,
)

@HiltViewModel
class SessionListViewModel @Inject constructor(
    private val repository: TouchSessionRepository,
) : ViewModel() {

    val uiState: StateFlow<SessionListUiState> = repository.observeSessions()
        .map { SessionListUiState(sessions = it, isLoaded = true) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = SessionListUiState(),
        )

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch { repository.deleteSession(sessionId) }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

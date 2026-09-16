package com.mandro.touchtracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mandro.touchtracker.model.CaptureSettings
import com.mandro.touchtracker.model.CoordinateUnit
import com.mandro.touchtracker.data.local.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<CaptureSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = CaptureSettings.DEFAULT,
    )

    fun setGridSpacing(mm: Float) = update { it.copy(gridSpacingMm = mm) }

    fun setCoordinateUnit(unit: CoordinateUnit) = update { it.copy(coordinateUnit = unit) }

    fun setShowTrail(enabled: Boolean) = update { it.copy(showTrail = enabled) }

    fun setKeepScreenOn(enabled: Boolean) = update { it.copy(keepScreenOn = enabled) }

    fun setRecordMoveEvents(enabled: Boolean) = update { it.copy(recordMoveEvents = enabled) }

    fun setIgnoreSyntheticInput(enabled: Boolean) = update { it.copy(ignoreSyntheticInput = enabled) }

    private fun update(transform: (CaptureSettings) -> CaptureSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

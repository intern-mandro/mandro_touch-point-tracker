package com.mandro.touchtracker.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mandro.touchtracker.model.CaptureSettings
import com.mandro.touchtracker.model.CoordinateUnit
import com.mandro.touchtracker.data.local.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class SettingsStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<CaptureSettings> = dataStore.data.map { it.toSettings() }

    override suspend fun update(transform: (CaptureSettings) -> CaptureSettings) {
        val current = settings.first()
        val next = transform(current)
        dataStore.edit { prefs ->
            prefs[KEY_GRID_SPACING_MM] = next.gridSpacingMm
                .coerceIn(CaptureSettings.MIN_GRID_SPACING_MM, CaptureSettings.MAX_GRID_SPACING_MM)
            prefs[KEY_COORDINATE_UNIT] = next.coordinateUnit.name
            prefs[KEY_SHOW_TRAIL] = next.showTrail
            prefs[KEY_KEEP_SCREEN_ON] = next.keepScreenOn
            prefs[KEY_RECORD_MOVE] = next.recordMoveEvents
            prefs[KEY_IGNORE_SYNTHETIC] = next.ignoreSyntheticInput
            // liveBufferSize 는 일부러 저장하지 않는다 — 아래 toSettings 설명 참고.
        }
    }

    private fun Preferences.toSettings(): CaptureSettings {
        val default = CaptureSettings.DEFAULT
        return CaptureSettings(
            gridSpacingMm = CaptureSettings.FIXED_GRID_SPACING_MM,
            // 저장된 이름이 사라진 enum 상수일 수 있다 (앱 다운그레이드 등) → 기본값으로 떨어뜨린다.
            coordinateUnit = this[KEY_COORDINATE_UNIT]
                ?.let { name -> CoordinateUnit.entries.firstOrNull { it.name == name } }
                ?: default.coordinateUnit,
            showTrail = this[KEY_SHOW_TRAIL] ?: default.showTrail,
            keepScreenOn = this[KEY_KEEP_SCREEN_ON] ?: default.keepScreenOn,
            recordMoveEvents = this[KEY_RECORD_MOVE] ?: default.recordMoveEvents,
            ignoreSyntheticInput = this[KEY_IGNORE_SYNTHETIC] ?: default.ignoreSyntheticInput,
            // 사용자가 바꿀 수 있는 항목이 아니라 메모리 상한 상수다. 저장해 두면
            // 상수를 고쳐도 예전 값이 살아남아 덮어쓴다 — 실제로 이미 깔린 기기에
            // 9 가 박혀 있어서 측정이 9 점만 저장되는 원인이 됐다. 항상 상수를 쓴다.
            liveBufferSize = default.liveBufferSize,
        )
    }

    private companion object {
        val KEY_GRID_SPACING_MM = floatPreferencesKey("grid_spacing_mm")
        val KEY_COORDINATE_UNIT = stringPreferencesKey("coordinate_unit")
        val KEY_SHOW_TRAIL = booleanPreferencesKey("show_trail")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEY_RECORD_MOVE = booleanPreferencesKey("record_move_events")
        val KEY_IGNORE_SYNTHETIC = booleanPreferencesKey("ignore_synthetic_input")
    }
}

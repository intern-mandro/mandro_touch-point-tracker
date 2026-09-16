package com.mandro.touchtracker.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mandro.touchtracker.model.CaptureSettings
import com.mandro.touchtracker.model.CoordinateUnit
import com.mandro.touchtracker.data.local.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 캡처 설정 영속화. Preferences DataStore 를 쓴다 — 항목이 몇 개 안 되고
 * 구조가 평평해서 직렬화 스키마를 둘 이유가 없다.
 */
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
            prefs[KEY_LIVE_BUFFER] = next.liveBufferSize.coerceAtLeast(CaptureSettings.RECENT_POINT_LIMIT)
        }
    }

    private fun Preferences.toSettings(): CaptureSettings {
        val default = CaptureSettings.DEFAULT
        return CaptureSettings(
            gridSpacingMm = this[KEY_GRID_SPACING_MM] ?: default.gridSpacingMm,
            // 저장된 이름이 사라진 enum 상수일 수 있다 (앱 다운그레이드 등) → 기본값으로 떨어뜨린다.
            coordinateUnit = this[KEY_COORDINATE_UNIT]
                ?.let { name -> CoordinateUnit.entries.firstOrNull { it.name == name } }
                ?: default.coordinateUnit,
            showTrail = this[KEY_SHOW_TRAIL] ?: default.showTrail,
            keepScreenOn = this[KEY_KEEP_SCREEN_ON] ?: default.keepScreenOn,
            recordMoveEvents = this[KEY_RECORD_MOVE] ?: default.recordMoveEvents,
            ignoreSyntheticInput = this[KEY_IGNORE_SYNTHETIC] ?: default.ignoreSyntheticInput,
            liveBufferSize = this[KEY_LIVE_BUFFER] ?: default.liveBufferSize,
        )
    }

    private companion object {
        val KEY_GRID_SPACING_MM = floatPreferencesKey("grid_spacing_mm")
        val KEY_COORDINATE_UNIT = stringPreferencesKey("coordinate_unit")
        val KEY_SHOW_TRAIL = booleanPreferencesKey("show_trail")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEY_RECORD_MOVE = booleanPreferencesKey("record_move_events")
        val KEY_IGNORE_SYNTHETIC = booleanPreferencesKey("ignore_synthetic_input")
        val KEY_LIVE_BUFFER = intPreferencesKey("live_buffer_size")
    }
}

package com.mandro.touchtracker.data.local

import com.mandro.touchtracker.model.CaptureSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<CaptureSettings>

    /** 부분 수정. 현재 값을 읽어 [transform] 을 먹인 결과를 저장한다. */
    suspend fun update(transform: (CaptureSettings) -> CaptureSettings)
}

package com.mandro.touchtracker.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    private const val SETTINGS_NAME = "capture_settings"

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) {
        context.preferencesDataStoreFile(SETTINGS_NAME)
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // 스키마가 늘어난 파일을 옛 앱이 읽어도 죽지 않게.
        ignoreUnknownKeys = true
        // 기본값도 파일에 남긴다 — 분석 스크립트가 필드 유무를 신경 쓰지 않도록.
        encodeDefaults = true
        prettyPrint = false
    }
}

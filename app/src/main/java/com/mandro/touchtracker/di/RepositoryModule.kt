package com.mandro.touchtracker.di

import com.mandro.touchtracker.core.time.Clock
import com.mandro.touchtracker.core.time.SystemClock
import com.mandro.touchtracker.data.export.SessionExporterImpl
import com.mandro.touchtracker.data.local.SettingsStore
import com.mandro.touchtracker.data.repository.TouchSessionRepositoryImpl
import com.mandro.touchtracker.data.export.SessionExporter
import com.mandro.touchtracker.data.local.SettingsRepository
import com.mandro.touchtracker.data.repository.TouchSessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTouchSessionRepository(impl: TouchSessionRepositoryImpl): TouchSessionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsStore): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSessionExporter(impl: SessionExporterImpl): SessionExporter

    companion object {
        @Provides
        @Singleton
        fun provideClock(): Clock = SystemClock()
    }
}

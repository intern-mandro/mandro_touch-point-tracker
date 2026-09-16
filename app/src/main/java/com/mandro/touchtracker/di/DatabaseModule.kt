package com.mandro.touchtracker.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mandro.touchtracker.data.db.SessionDao
import com.mandro.touchtracker.data.db.TouchDatabase
import com.mandro.touchtracker.data.db.TouchPointDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TouchDatabase =
        Room.databaseBuilder(context, TouchDatabase::class.java, TouchDatabase.NAME)
            // 기록 중에는 터치 포인트가 계속 insert 된다. WAL 이면 그 와중에도
            // 화면의 조회 쿼리가 쓰기를 기다리지 않는다.
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()

    @Provides
    fun provideSessionDao(db: TouchDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideTouchPointDao(db: TouchDatabase): TouchPointDao = db.touchPointDao()

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver
}

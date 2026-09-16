package com.mandro.touchtracker.di

import javax.inject.Qualifier

/** 앱이 살아 있는 동안 유지되는 스코프. ViewModel 수명과 무관한 수집 루프에 쓴다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** 디스크·DB 작업용 디스패처. 테스트에서 교체할 수 있게 주입받는다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** 기본 연산용 디스패처 (좌표 통계 등). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

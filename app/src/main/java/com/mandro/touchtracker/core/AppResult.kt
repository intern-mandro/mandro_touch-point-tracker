package com.mandro.touchtracker.core

/**
 * 사용자에게 성공/실패를 그대로 보여줘야 하는 작업(내보내기 등)의 결과.
 *
 * 그 외 내부 호출은 그냥 예외를 던지거나 nullable 을 쓴다 — 전 계층을 래핑하지 않는다.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>

    /**
     * @param message UI 에 그대로 띄울 수 있는 한국어 문장
     * @param cause   로그용 원인. UI 에 노출하지 않는다.
     */
    data class Failure(val message: String, val cause: Throwable? = null) : AppResult<Nothing>
}

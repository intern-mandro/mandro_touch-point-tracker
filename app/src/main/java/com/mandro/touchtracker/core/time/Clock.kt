package com.mandro.touchtracker.core.time

/**
 * 시간 소스. 테스트에서 시각을 고정하려고 인터페이스로 뺐다.
 *
 * - [epochMs]   : 사람이 읽는 절대 시각. 파일 이름·세션 표시에 쓴다.
 * - [uptimeMs]  : 단조 증가 시각. MotionEvent.eventTime 과 같은 기준이라
 *                 터치 간 간격 계산은 반드시 이쪽을 쓴다 (시계 변경에 안 흔들림).
 */
interface Clock {
    fun epochMs(): Long
    fun uptimeMs(): Long
}

class SystemClock : Clock {
    override fun epochMs(): Long = System.currentTimeMillis()
    override fun uptimeMs(): Long = android.os.SystemClock.uptimeMillis()
}

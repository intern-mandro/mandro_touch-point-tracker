package com.mandro.touchtracker.data.repository

import com.mandro.touchtracker.model.DeviceProfile
import com.mandro.touchtracker.model.TouchPoint
import com.mandro.touchtracker.model.TouchSession
import kotlinx.coroutines.flow.Flow

/**
 * 세션·터치포인트 저장 계약. ViewModel과 수집기는 Room 구현을 직접 알 필요가 없다.
 * 공통 비즈니스 로직이 복잡해지기 전까지 호출자는 이 계약을 바로 사용한다.
 */
interface TouchSessionRepository {

    fun observeSessions(): Flow<List<TouchSession>>

    fun observeSession(sessionId: Long): Flow<TouchSession?>

    fun observePoints(sessionId: Long): Flow<List<TouchPoint>>

    suspend fun getSession(sessionId: Long): TouchSession?

    /** 새 세션을 열고 id 를 돌려준다. 이 시점의 [device] 가 세션 좌표계로 고정된다. */
    suspend fun startSession(name: String, note: String, device: DeviceProfile): Long

    /** 종료 시각을 찍는다. 이미 닫힌 세션에 다시 불러도 안전해야 한다. */
    suspend fun endSession(sessionId: Long)

    /**
     * 점을 묶음으로 저장한다. MOVE 는 초당 수백 개가 들어오므로 한 건씩 넣지 않는다.
     * 호출자가 [TouchPoint.sequence] 의 연속성을 보장한다.
     */
    suspend fun appendPoints(points: List<TouchPoint>)

    suspend fun renameSession(sessionId: Long, name: String, note: String)

    suspend fun deleteSession(sessionId: Long)
}

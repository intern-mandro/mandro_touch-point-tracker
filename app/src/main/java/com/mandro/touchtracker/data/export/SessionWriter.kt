package com.mandro.touchtracker.data.export

import com.mandro.touchtracker.model.TouchPoint
import com.mandro.touchtracker.model.TouchSession
import java.io.Writer

/**
 * 세션을 한 번에 다 메모리에 올리지 않고 흘려 쓰기 위한 계약.
 * 호출 순서는 항상 [begin] → [writeChunk]* → [end] 다.
 *
 * 안드로이드 의존이 없어서 JVM 단위테스트로 출력 형식을 그대로 검증할 수 있다.
 */
interface SessionWriter {
    fun begin(out: Writer, session: TouchSession)
    fun writeChunk(out: Writer, session: TouchSession, points: List<TouchPoint>)
    fun end(out: Writer)
}

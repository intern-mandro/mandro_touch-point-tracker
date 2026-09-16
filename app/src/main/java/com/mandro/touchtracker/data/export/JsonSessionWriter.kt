package com.mandro.touchtracker.data.export

import com.mandro.touchtracker.model.TouchPoint
import com.mandro.touchtracker.model.TouchSession
import kotlinx.serialization.json.Json
import java.io.Writer

/**
 * 재현·재생용 JSON. 세션 메타와 점을 한 파일에 담는다.
 *
 * CSV 와 달리 좌표계 설정을 따르지 않는다 — px·mm·정규화를 전부 싣는다.
 * 이쪽은 "나중에 무엇이 필요할지 모르는 보관본"이라 정보를 깎지 않는다.
 *
 * 배열 구분자(`[`, `,`, `]`)만 직접 찍고 각 원소는 kotlinx.serialization 이 만든다.
 * 문서 전체를 객체로 쌓았다가 한 번에 인코딩하면 큰 세션에서 메모리를 그대로 먹기 때문이다
 * — 이스케이프 같은 진짜 까다로운 부분은 직렬화 라이브러리에 맡긴 채로 흘려 쓴다.
 */
class JsonSessionWriter(
    private val json: Json,
) : SessionWriter {

    private var wroteAnyPoint = false

    override fun begin(out: Writer, session: TouchSession) {
        wroteAnyPoint = false
        out.append("{\"formatVersion\":").append(FORMAT_VERSION.toString())
        out.append(",\"session\":")
        out.append(json.encodeToString(SessionMetaDto.serializer(), session.toMetaDto()))
        out.append(",\"points\":[")
    }

    override fun writeChunk(out: Writer, session: TouchSession, points: List<TouchPoint>) {
        for (p in points) {
            if (wroteAnyPoint) out.append(',')
            out.append(json.encodeToString(TouchPointDto.serializer(), p.toDto(session)))
            wroteAnyPoint = true
        }
    }

    override fun end(out: Writer) {
        out.append("]}")
    }
}

package com.mandro.touchtracker.data.export

import com.google.common.truth.Truth.assertThat
import com.mandro.touchtracker.core.geometry.ScreenMetrics
import com.mandro.touchtracker.model.DeviceProfile
import com.mandro.touchtracker.model.TouchPhase
import com.mandro.touchtracker.model.TouchPoint
import com.mandro.touchtracker.model.TouchSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.StringWriter

/**
 * 내보낸 파일은 분석 스크립트가 붙잡고 있는 계약이다. 형식이 조용히 바뀌면
 * 며칠 뒤 파이썬 쪽에서 터지므로 여기서 잠가 둔다.
 */
class SessionWriterTest {

    private val session = TouchSession(
        id = 7L,
        name = "grip-A",
        note = "3차 시도",
        startedAtEpochMs = 1_757_000_000_000L,
        endedAtEpochMs = null,
        device = DeviceProfile(
            model = "SM-S911N",
            manufacturer = "samsung",
            androidSdk = 35,
            metrics = ScreenMetrics(1000, 2000, 254f, 254f, 2.5f),
        ),
    )

    private val points = listOf(
        touchPoint(sequence = 0, xPx = 100f, yPx = 200f, phase = TouchPhase.DOWN),
        touchPoint(sequence = 1, xPx = 105f, yPx = 210f, phase = TouchPhase.UP),
    )

    @Test
    fun `csv keeps the measurement context in comment lines above the header`() {
        // Arrange
        val out = StringWriter()
        val writer = CsvSessionWriter()

        // Act
        writer.begin(out, session)
        writer.end(out)

        // Assert — 주석은 pandas 가 comment='#' 로 건너뛴다.
        val lines = out.toString().trim().lines()
        assertThat(lines.dropLast(1).all { it.startsWith("#") }).isTrue()
        assertThat(lines).contains("# screen_px=1000x2000 xdpi=254.0 ydpi=254.0 physical_dpi_trusted=true")
        assertThat(lines.last()).isEqualTo(CsvSessionWriter.HEADER)
    }

    @Test
    fun `csv writes one row per point with px mm and normalized coordinates`() {
        val out = StringWriter()
        val writer = CsvSessionWriter()

        writer.begin(out, session)
        writer.writeChunk(out, session, points)
        writer.end(out)

        val dataRows = out.toString().trim().lines().filterNot { it.startsWith("#") }.drop(1)
        assertThat(dataRows).hasSize(2)

        val first = dataRows.first().split(",")
        assertThat(first[HEADER_INDEX_SEQUENCE]).isEqualTo("0")
        assertThat(first[HEADER_INDEX_PHASE]).isEqualTo("DOWN")
        assertThat(first[HEADER_INDEX_X_PX]).isEqualTo("100.0")
        // 254 dpi → 10 px = 1 mm
        assertThat(first[HEADER_INDEX_X_MM].toFloat()).isWithin(TOLERANCE).of(10f)
        assertThat(first[HEADER_INDEX_X_NORM].toFloat()).isWithin(TOLERANCE).of(0.1f)
    }

    @Test
    fun `json stays parseable when points arrive in several chunks`() {
        // Arrange — 내보내기는 페이지 단위로 흘려 쓴다. 청크 경계에서 쉼표가
        // 빠지거나 겹치는 게 이 형식의 유일한 위험 지점이다.
        val out = StringWriter()
        val writer = JsonSessionWriter(Json)

        // Act
        writer.begin(out, session)
        writer.writeChunk(out, session, listOf(points[0]))
        writer.writeChunk(out, session, listOf(points[1]))
        writer.end(out)

        // Assert
        val document = Json.parseToJsonElement(out.toString()).jsonObject
        assertThat(document["formatVersion"]?.jsonPrimitive?.content).isEqualTo("$FORMAT_VERSION")
        assertThat(document["session"]?.jsonObject?.get("name")?.jsonPrimitive?.content).isEqualTo("grip-A")
        assertThat(document["points"]?.jsonArray).hasSize(2)
    }

    @Test
    fun `json emits an empty array when the session has no points`() {
        val out = StringWriter()
        val writer = JsonSessionWriter(Json)

        writer.begin(out, session)
        writer.end(out)

        val document = Json.parseToJsonElement(out.toString()).jsonObject
        assertThat(document["points"]?.jsonArray).isEmpty()
    }

    private fun touchPoint(sequence: Int, xPx: Float, yPx: Float, phase: TouchPhase) = TouchPoint(
        sessionId = session.id,
        sequence = sequence,
        pointerId = 0,
        phase = phase,
        xPx = xPx,
        yPx = yPx,
        pressure = 0.5f,
        touchMajorPx = 40f,
        touchMinorPx = 35f,
        orientationRad = 0f,
        elapsedMs = sequence * 100L,
        epochMs = session.startedAtEpochMs + sequence * 100L,
    )

    private companion object {
        const val TOLERANCE = 0.001f

        // CsvSessionWriter.HEADER 의 열 순서.
        const val HEADER_INDEX_SEQUENCE = 0
        const val HEADER_INDEX_PHASE = 2
        const val HEADER_INDEX_X_PX = 3
        const val HEADER_INDEX_X_MM = 5
        const val HEADER_INDEX_X_NORM = 7
    }
}

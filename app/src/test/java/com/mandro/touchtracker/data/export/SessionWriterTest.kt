package com.mandro.touchtracker.data.export

import com.google.common.truth.Truth.assertThat
import com.mandro.touchtracker.core.geometry.ScreenMetrics
import com.mandro.touchtracker.model.CoordinateUnit
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

    private val session = session()

    private val points = listOf(
        touchPoint(sequence = 0, xPx = 100f, yPx = 200f, phase = TouchPhase.DOWN),
        touchPoint(sequence = 1, xPx = 105f, yPx = 210f, phase = TouchPhase.UP),
    )

    @Test
    fun `csv keeps the measurement context in comment lines above the header`() {
        // Arrange
        val out = StringWriter()
        val writer = CsvSessionWriter(CoordinateUnit.PX)

        // Act
        writer.begin(out, session)
        writer.end(out)

        // Assert — 주석은 pandas 가 comment='#' 로 건너뛴다.
        val lines = out.toString().trim().lines()
        assertThat(lines.dropLast(1).all { it.startsWith("#") }).isTrue()
        assertThat(lines).contains("# screen_px=1000x2000 xdpi=254.0 ydpi=254.0 physical_dpi_trusted=true")
        assertThat(lines).contains("# coordinate_unit=PX")
        assertThat(lines.last()).isEqualTo(CsvSessionWriter.header(CoordinateUnit.PX))
    }

    @Test
    fun `csv writes px coordinates when the px unit is selected`() {
        val rows = writeCsvRows(CoordinateUnit.PX)

        assertThat(headerOf(CoordinateUnit.PX)).contains("x_px,y_px")
        assertThat(rows.first()[COLUMN_X].toFloat()).isWithin(TOLERANCE).of(100f)
        assertThat(rows.first()[COLUMN_Y].toFloat()).isWithin(TOLERANCE).of(200f)
    }

    @Test
    fun `csv converts coordinates to millimetres when the mm unit is selected`() {
        // Arrange — 254 dpi 라 10 px = 1 mm 로 딱 떨어진다.
        val rows = writeCsvRows(CoordinateUnit.MM)

        // Assert — 열 이름도 같이 바뀌어야 단위를 나중에 알아볼 수 있다.
        assertThat(headerOf(CoordinateUnit.MM)).contains("x_mm,y_mm")
        assertThat(rows.first()[COLUMN_X].toFloat()).isWithin(TOLERANCE).of(10f)
        assertThat(rows.first()[COLUMN_Y].toFloat()).isWithin(TOLERANCE).of(20f)
    }

    @Test
    fun `csv converts coordinates to the zero to one range when normalized is selected`() {
        val rows = writeCsvRows(CoordinateUnit.NORMALIZED)

        assertThat(headerOf(CoordinateUnit.NORMALIZED)).contains("x_norm,y_norm")
        assertThat(rows.first()[COLUMN_X].toFloat()).isWithin(TOLERANCE).of(0.1f)
        assertThat(rows.first()[COLUMN_Y].toFloat()).isWithin(TOLERANCE).of(0.1f)
    }

    @Test
    fun `csv writes exactly one coordinate pair regardless of unit`() {
        // 예전 형식은 px·mm·정규화를 한 줄에 다 실었다. 설정 단위 하나만 나가야
        // 화면에서 보던 값과 파일이 일치한다.
        val rows = writeCsvRows(CoordinateUnit.MM)

        assertThat(rows.first()).hasSize(COLUMN_COUNT)
        assertThat(headerOf(CoordinateUnit.MM).split(",")).hasSize(COLUMN_COUNT)
        assertThat(headerOf(CoordinateUnit.MM)).doesNotContain("x_px")
        assertThat(headerOf(CoordinateUnit.MM)).doesNotContain("x_norm")
    }

    @Test
    fun `csv warns loudly when mm is requested but the device has no usable dpi`() {
        // Arrange — xdpi/ydpi 를 0 으로 보내는 기기가 실제로 있다. 이때 환산이
        // 일어나지 않아 px 가 mm 인 척 파일에 남는다.
        val out = StringWriter()
        val untrusted = session(metrics = ScreenMetrics(1000, 2000, 0f, 0f, 2.5f))

        // Act
        CsvSessionWriter(CoordinateUnit.MM).begin(out, untrusted)

        // Assert
        assertThat(out.toString().lines()).contains(CsvSessionWriter.DPI_WARNING)
    }

    @Test
    fun `csv stays silent about dpi when millimetres are trustworthy`() {
        val out = StringWriter()

        CsvSessionWriter(CoordinateUnit.MM).begin(out, session)

        assertThat(out.toString()).doesNotContain("WARNING")
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
    fun `json keeps every coordinate system regardless of the display setting`() {
        // JSON 은 보관본이다. CSV 와 달리 설정을 따르지 않고 정보를 깎지 않는다.
        val out = StringWriter()
        val writer = JsonSessionWriter(Json)

        writer.begin(out, session)
        writer.writeChunk(out, session, listOf(points[0]))
        writer.end(out)

        val first = Json.parseToJsonElement(out.toString())
            .jsonObject["points"]!!.jsonArray.first().jsonObject
        assertThat(first.keys).containsAtLeast("xPx", "xMm", "xNorm")
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

    /** 주석·헤더를 걷어낸 데이터 줄만 열 단위로 쪼개 준다. */
    private fun writeCsvRows(unit: CoordinateUnit): List<List<String>> {
        val out = StringWriter()
        val writer = CsvSessionWriter(unit)

        writer.begin(out, session)
        writer.writeChunk(out, session, points)
        writer.end(out)

        return out.toString().trim().lines()
            .filterNot { it.startsWith("#") }
            .drop(1)
            .map { it.split(",") }
    }

    private fun headerOf(unit: CoordinateUnit): String = CsvSessionWriter.header(unit)

    private fun session(metrics: ScreenMetrics = ScreenMetrics(1000, 2000, 254f, 254f, 2.5f)) =
        TouchSession(
            id = 7L,
            name = "grip-A",
            note = "3차 시도",
            startedAtEpochMs = 1_757_000_000_000L,
            endedAtEpochMs = null,
            device = DeviceProfile(
                model = "SM-S911N",
                manufacturer = "samsung",
                androidSdk = 35,
                metrics = metrics,
            ),
        )

    private fun touchPoint(sequence: Int, xPx: Float, yPx: Float, phase: TouchPhase) = TouchPoint(
        sessionId = 7L,
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
        epochMs = 1_757_000_000_000L + sequence * 100L,
    )

    private companion object {
        const val TOLERANCE = 0.001f

        // header(unit) 의 열 순서.
        const val COLUMN_X = 3
        const val COLUMN_Y = 4
        const val COLUMN_COUNT = 11
    }
}

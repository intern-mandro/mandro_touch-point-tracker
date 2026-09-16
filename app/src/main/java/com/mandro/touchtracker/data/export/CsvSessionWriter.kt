package com.mandro.touchtracker.data.export

import com.mandro.touchtracker.core.geometry.ScreenMetrics
import com.mandro.touchtracker.model.CoordinateUnit
import com.mandro.touchtracker.model.TouchPoint
import com.mandro.touchtracker.model.TouchSession
import java.io.Writer

/**
 * 엑셀·pandas 로 바로 여는 CSV.
 *
 * 좌표는 **설정의 [CoordinateUnit] 하나로만** 쓴다. 세 좌표계를 항상 다 싣지 않는
 * 이유는, 화면에서 보던 단위와 파일에 찍힌 단위가 같아야 받아 적은 값과 대조가
 * 되기 때문이다. 다른 단위가 필요하면 설정을 바꿔서 다시 내보내거나, 세 좌표계를
 * 모두 담는 JSON 을 쓴다.
 *
 * 단위가 무엇이었는지는 열 이름(`x_px` / `x_mm` / `x_norm`)과 맨 위 `#` 주석
 * 양쪽에 남는다. 좌표계를 모르는 숫자 표는 나중에 해석이 안 된다.
 * pandas 는 `read_csv(path, comment='#')` 로 주석을 건너뛴다.
 */
class CsvSessionWriter(
    private val coordinateUnit: CoordinateUnit,
) : SessionWriter {

    override fun begin(out: Writer, session: TouchSession) {
        val m = session.metrics
        out.append("# mandro touch-point-tracker export v").append(FORMAT_VERSION.toString()).append(NL)
        out.append("# session=").append(session.name).append(NL)
        out.append("# note=").append(session.note.replace('\n', ' ')).append(NL)
        out.append("# device=").append(session.device.manufacturer).append(' ')
            .append(session.device.model).append(" sdk=").append(session.device.androidSdk.toString()).append(NL)
        out.append("# screen_px=").append("${m.widthPx}x${m.heightPx}")
            .append(" xdpi=").append(m.xDpi.toString())
            .append(" ydpi=").append(m.yDpi.toString())
            .append(" physical_dpi_trusted=").append(m.hasPhysicalDpi.toString()).append(NL)
        out.append("# coordinate_unit=").append(coordinateUnit.name).append(NL)

        // mm 를 골랐는데 기기가 DPI 를 제대로 안 알려주면 ScreenMetrics 가 환산을
        // 포기하고 px 를 그대로 돌려준다. 그 값이 mm 인 척 파일에 남으면
        // 분석 단계에서 알아챌 방법이 없으므로 여기서 크게 적어 둔다.
        if (coordinateUnit == CoordinateUnit.MM && !m.hasPhysicalDpi) {
            out.append(DPI_WARNING).append(NL)
        }

        out.append("# started_at_epoch_ms=").append(session.startedAtEpochMs.toString()).append(NL)
        out.append(header(coordinateUnit)).append(NL)
    }

    override fun writeChunk(out: Writer, session: TouchSession, points: List<TouchPoint>) {
        val m = session.metrics
        for (p in points) {
            out.append(p.sequence.toString()).append(SEP)
                .append(p.pointerId.toString()).append(SEP)
                .append(p.phase.name).append(SEP)
                .append(convertX(p, m).toString()).append(SEP)
                .append(convertY(p, m).toString()).append(SEP)
                .append(p.pressure.toString()).append(SEP)
                .append(p.touchMajorPx.toString()).append(SEP)
                .append(p.touchMinorPx.toString()).append(SEP)
                .append(p.orientationRad.toString()).append(SEP)
                .append(p.elapsedMs.toString()).append(SEP)
                .append(p.epochMs.toString()).append(NL)
        }
    }

    override fun end(out: Writer) = Unit

    /**
     * 화면 표시와 달리 반올림하지 않는다. 분석 파일에서 자릿수를 깎으면 되돌릴 수
     * 없고, 애초에 잘라야 할 정밀도는 분석 쪽에서 정할 일이다.
     */
    private fun convertX(point: TouchPoint, metrics: ScreenMetrics): Float = when (coordinateUnit) {
        CoordinateUnit.PX -> point.xPx
        CoordinateUnit.MM -> metrics.toMmX(point.xPx)
        CoordinateUnit.NORMALIZED -> metrics.toNormalizedX(point.xPx)
    }

    private fun convertY(point: TouchPoint, metrics: ScreenMetrics): Float = when (coordinateUnit) {
        CoordinateUnit.PX -> point.yPx
        CoordinateUnit.MM -> metrics.toMmY(point.yPx)
        CoordinateUnit.NORMALIZED -> metrics.toNormalizedY(point.yPx)
    }

    companion object {
        const val SEP = ","
        const val NL = "\n"

        const val DPI_WARNING =
            "# WARNING: this device reports no usable DPI — x_mm/y_mm are raw px, NOT millimetres"

        /**
         * 접촉 크기는 단위와 무관하게 언제나 px 다. 정규화 좌표계에는 길이라는
         * 개념이 없어서 같이 환산할 수가 없고, 열 이름을 고정해 둬야 분석
         * 스크립트가 단위를 바꿔도 그대로 돈다.
         */
        private const val TAIL_COLUMNS =
            "pressure,touch_major_px,touch_minor_px,orientation_rad,elapsed_ms,epoch_ms"

        /** 좌표 열 이름이 곧 단위 표기다. `x_px` / `x_mm` / `x_norm`. */
        fun header(unit: CoordinateUnit): String {
            val suffix = columnSuffix(unit)
            return "sequence,pointer_id,phase,x_$suffix,y_$suffix,$TAIL_COLUMNS"
        }

        fun columnSuffix(unit: CoordinateUnit): String = when (unit) {
            CoordinateUnit.PX -> "px"
            CoordinateUnit.MM -> "mm"
            CoordinateUnit.NORMALIZED -> "norm"
        }
    }
}

package com.mandro.touchtracker.data.export

import com.mandro.touchtracker.model.TouchPoint
import com.mandro.touchtracker.model.TouchSession
import java.io.Writer
import javax.inject.Inject

/**
 * 엑셀·pandas 로 바로 여는 CSV.
 *
 * 맨 위 `#` 주석 줄에 측정 조건(기기·좌표계)을 남긴다. CSV 자체에는 메타를 담을
 * 자리가 없는데, 좌표계를 모르는 px 표는 나중에 해석이 안 되기 때문이다.
 * pandas 는 `read_csv(path, comment='#')` 로 그냥 건너뛴다.
 */
class CsvSessionWriter @Inject constructor() : SessionWriter {

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
        out.append("# started_at_epoch_ms=").append(session.startedAtEpochMs.toString()).append(NL)
        out.append(HEADER).append(NL)
    }

    override fun writeChunk(out: Writer, session: TouchSession, points: List<TouchPoint>) {
        val m = session.metrics
        for (p in points) {
            out.append(p.sequence.toString()).append(SEP)
                .append(p.pointerId.toString()).append(SEP)
                .append(p.phase.name).append(SEP)
                .append(p.xPx.toString()).append(SEP)
                .append(p.yPx.toString()).append(SEP)
                .append(m.toMmX(p.xPx).toString()).append(SEP)
                .append(m.toMmY(p.yPx).toString()).append(SEP)
                .append(m.toNormalizedX(p.xPx).toString()).append(SEP)
                .append(m.toNormalizedY(p.yPx).toString()).append(SEP)
                .append(p.pressure.toString()).append(SEP)
                .append(p.touchMajorPx.toString()).append(SEP)
                .append(p.touchMinorPx.toString()).append(SEP)
                .append(p.orientationRad.toString()).append(SEP)
                .append(p.elapsedMs.toString()).append(SEP)
                .append(p.epochMs.toString()).append(NL)
        }
    }

    override fun end(out: Writer) = Unit

    companion object {
        const val SEP = ","
        const val NL = "\n"

        const val HEADER = "sequence,pointer_id,phase,x_px,y_px,x_mm,y_mm,x_norm,y_norm," +
            "pressure,touch_major_px,touch_minor_px,orientation_rad,elapsed_ms,epoch_ms"
    }
}

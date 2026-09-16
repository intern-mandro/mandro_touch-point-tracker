package com.mandro.touchtracker.ui.capture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mandro.touchtracker.model.TouchPhase
import com.mandro.touchtracker.model.TouchPoint
import com.mandro.touchtracker.ui.theme.LocalTrackerColors
import com.mandro.touchtracker.ui.theme.TouchTrackerTheme

/**
 * 탭 ②: 모눈종이 시각화.
 *
 * 화면 전체를 비율 그대로 축소해 격자 위에 얹고, 기록된 터치를 점으로 찍는다.
 * 가장 최근 점만 십자선과 접촉 고리를 달아 "방금 어디를 눌렀는지"를 즉시 읽게 한다.
 */
@Composable
fun GraphPaperTab(
    state: CaptureUiState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTrackerColors.current
    val paper = MaterialTheme.colorScheme.surface

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(CANVAS_PADDING_DP.dp),
        ) {
            val projection = ScreenProjection.fit(state.metrics, size)
            drawPaper(projection, paper)
            drawGrid(projection, state.settings.gridSpacingMm, colors.gridLine, colors.gridLineMajor)
            if (state.settings.showTrail) drawTrail(projection, state.livePoints, colors.trace)
            drawOlderPoints(projection, state.livePoints, colors.trace)
            state.latestPoint?.let { drawLatestPoint(projection, it, colors.marker) }
        }

        if (!state.hasPoints) {
            Text(
                text = if (state.isRecording) EMPTY_RECORDING else EMPTY_IDLE,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/** 화면 영역 자체. 여백(letterbox)과 측정 영역을 눈으로 갈라 준다. */
private fun DrawScope.drawPaper(projection: ScreenProjection, paper: Color) {
    drawRect(
        color = paper,
        topLeft = Offset(projection.originX, projection.originY),
        size = Size(projection.projectedWidth, projection.projectedHeight),
    )
}

/**
 * 모눈. [spacingMm] 마다 가는 선, [MAJOR_EVERY] 칸마다 굵은 선.
 *
 * 격자 간격을 mm 로 잡는 게 핵심이다. px 로 잡으면 기기가 바뀔 때마다 한 칸의 실제
 * 크기가 달라져서, 눈으로 센 "몇 칸 벗어났다"를 기기 간에 비교할 수 없다.
 */
private fun DrawScope.drawGrid(
    projection: ScreenProjection,
    spacingMm: Float,
    minorColor: Color,
    majorColor: Color,
) {
    val step = projection.mmToCanvas(spacingMm)
    if (step < MIN_VISIBLE_STEP_PX) return // 너무 촘촘하면 격자가 아니라 얼룩이 된다

    val left = projection.originX
    val top = projection.originY
    val right = left + projection.projectedWidth
    val bottom = top + projection.projectedHeight

    var column = 0
    var x = left
    while (x <= right + HALF_PX) {
        val isMajor = column % MAJOR_EVERY == 0
        drawLine(
            color = if (isMajor) majorColor else minorColor,
            start = Offset(x, top),
            end = Offset(x, bottom),
            strokeWidth = if (isMajor) MAJOR_STROKE_PX else MINOR_STROKE_PX,
        )
        x += step
        column++
    }

    var row = 0
    var y = top
    while (y <= bottom + HALF_PX) {
        val isMajor = row % MAJOR_EVERY == 0
        drawLine(
            color = if (isMajor) majorColor else minorColor,
            start = Offset(left, y),
            end = Offset(right, y),
            strokeWidth = if (isMajor) MAJOR_STROKE_PX else MINOR_STROKE_PX,
        )
        y += step
        row++
    }

    drawRect(
        color = majorColor,
        topLeft = Offset(left, top),
        size = Size(projection.projectedWidth, projection.projectedHeight),
        style = Stroke(width = BORDER_STROKE_PX),
    )
}

/** 접촉 중 이동 궤적. DOWN 에서 UP 까지 이어진 선이 곧 미끄러짐(slip)이다. */
private fun DrawScope.drawTrail(
    projection: ScreenProjection,
    points: List<TouchPoint>,
    color: Color,
) {
    if (points.size < 2) return

    val path = Path()
    var hasCursor = false
    for (point in points) {
        val offset = projection.toCanvas(point.xPx, point.yPx)
        // DOWN 은 새 접촉의 시작이다. 직전 접촉과 선으로 이으면 허공을 가로지르는
        // 가짜 궤적이 생긴다.
        if (point.phase == TouchPhase.DOWN || !hasCursor) {
            path.moveTo(offset.x, offset.y)
            hasCursor = true
        } else {
            path.lineTo(offset.x, offset.y)
        }
    }
    drawPath(path, color.copy(alpha = TRAIL_ALPHA), style = Stroke(width = TRAIL_STROKE_PX))
}

/** 최신 점을 뺀 나머지. 오래될수록 옅게 그려 시간 순서를 색으로 읽게 한다. */
private fun DrawScope.drawOlderPoints(
    projection: ScreenProjection,
    points: List<TouchPoint>,
    color: Color,
) {
    if (points.size < 2) return

    val lastIndex = points.lastIndex
    for (index in 0 until lastIndex) {
        val point = points[index]
        val recency = index.toFloat() / lastIndex // 0 = 가장 오래됨
        val alpha = MIN_POINT_ALPHA + (1f - MIN_POINT_ALPHA) * recency
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = POINT_RADIUS_PX,
            center = projection.toCanvas(point.xPx, point.yPx),
        )
    }
}

/** 가장 최근 터치. 십자선이 격자 눈금까지 시선을 이어 준다. */
private fun DrawScope.drawLatestPoint(
    projection: ScreenProjection,
    point: TouchPoint,
    color: Color,
) {
    val center = projection.toCanvas(point.xPx, point.yPx)
    val left = projection.originX
    val top = projection.originY
    val right = left + projection.projectedWidth
    val bottom = top + projection.projectedHeight
    val crosshair = color.copy(alpha = CROSSHAIR_ALPHA)

    drawLine(crosshair, Offset(left, center.y), Offset(right, center.y), CROSSHAIR_STROKE_PX)
    drawLine(crosshair, Offset(center.x, top), Offset(center.x, bottom), CROSSHAIR_STROKE_PX)

    // 접촉 타원 크기를 그대로 반영한 고리 — 얼마나 넓게 눌렸는지가 보인다.
    val contactRadius = point.touchMajorPx / 2f * projection.scale
    if (contactRadius > MARKER_RADIUS_PX) {
        drawCircle(
            color = color.copy(alpha = CONTACT_ALPHA),
            radius = contactRadius,
            center = center,
            style = Stroke(width = MINOR_STROKE_PX),
        )
    }
    drawCircle(color, MARKER_RADIUS_PX, center)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun GraphPaperTabPreview() {
    TouchTrackerTheme {
        GraphPaperTab(state = CaptureUiState(livePoints = previewTouchPoints()))
    }
}

private const val EMPTY_RECORDING = "화면을 터치하면 여기에 찍힙니다"
private const val EMPTY_IDLE = "기록을 시작하세요"

private const val CANVAS_PADDING_DP = 12

private const val MAJOR_EVERY = 5
private const val MIN_VISIBLE_STEP_PX = 4f
private const val HALF_PX = 0.5f

private const val MINOR_STROKE_PX = 1f
private const val MAJOR_STROKE_PX = 1.8f
private const val BORDER_STROKE_PX = 2.5f
private const val TRAIL_STROKE_PX = 2f
private const val CROSSHAIR_STROKE_PX = 1.2f

private const val POINT_RADIUS_PX = 5f
private const val MARKER_RADIUS_PX = 8f

private const val MIN_POINT_ALPHA = 0.25f
private const val TRAIL_ALPHA = 0.5f
private const val CROSSHAIR_ALPHA = 0.65f
private const val CONTACT_ALPHA = 0.55f

package com.mandro.touchtracker.ui.capture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mandro.touchtracker.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mandro.touchtracker.core.geometry.ScreenMetrics
import com.mandro.touchtracker.model.CoordinateUnit
import com.mandro.touchtracker.model.TouchPhase
import com.mandro.touchtracker.model.TouchPoint
import com.mandro.touchtracker.ui.theme.LocalTrackerColors
import com.mandro.touchtracker.ui.theme.TouchTrackerTheme
import com.mandro.touchtracker.ui.theme.getMarkerColor
import com.mandro.touchtracker.ui.theme.getMarkerRimColor
import kotlin.math.roundToInt

import android.graphics.RectF
import androidx.compose.runtime.DisposableEffect

/**
 * 탭 ②: 모눈종이 시각화.
 *
 * 화면을 터치한 바로 그 자리에 1:1로 실시간 점과 십자선을 찍는다.
 */
@Composable
fun GraphPaperTab(
    state: CaptureUiState,
    modifier: Modifier = Modifier,
    onCanvasBoundsChanged: (RectF?) -> Unit = {},
) {
    val colors = LocalTrackerColors.current
    val paper = MaterialTheme.colorScheme.surface
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .onGloballyPositioned { coordinates ->
                    val pos = coordinates.positionInWindow()
                    val size = coordinates.size
                    onCanvasBoundsChanged(
                        RectF(
                            pos.x,
                            pos.y,
                            pos.x + size.width,
                            pos.y + size.height,
                        )
                    )
                },
        ) {
            val projection = ScreenProjection.direct(state.metrics)
            val displayPoints = state.recentLivePoints
            val metrics = state.metrics
            val unit = state.settings.coordinateUnit

            drawPaper(paper)
            drawGrid(projection, state.settings.gridSpacingMm, colors.gridLine)
            if (state.settings.showTrail) drawTrail(projection, displayPoints, colors.trace)
            drawOlderPoints(projection, displayPoints, metrics, unit, paper, textMeasurer)
            displayPoints.lastOrNull()?.let {
                drawLatestPoint(projection, it, metrics, unit, paper, textMeasurer)
            }
            drawResolutionReference(metrics, unit, colors.gridLineMajor, textMeasurer)
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

/** 캔버스 전체를 순백색 종이로 채운다. */
private fun DrawScope.drawPaper(paper: Color) {
    drawRect(color = paper, topLeft = Offset.Zero, size = size)
}

/**
 * 모눈. [spacingMm] 마다 균일한 선을 그린다.
 * 모눈종이 좌상단 (0, 0)을 원점으로 눈금을 맞춰 실제 mm 치수와 1:1로 일치시킨다.
 */
private fun DrawScope.drawGrid(
    projection: ScreenProjection,
    spacingMm: Float,
    gridColor: Color,
) {
    val step = projection.mmToCanvas(spacingMm)
    if (step < MIN_VISIBLE_STEP_PX) return

    val width = size.width
    val height = size.height

    var x = 0f
    while (x <= width + HALF_PX) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = GRID_STROKE_PX,
        )
        x += step
    }

    var y = 0f
    while (y <= height + HALF_PX) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = GRID_STROKE_PX,
        )
        y += step
    }
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

/** 최신 점을 뺀 나머지. 공(동그란 원 + 연한 테두리) 스타일과 상단 (X, Y) 좌표 뱃지로 그린다. */
private fun DrawScope.drawOlderPoints(
    projection: ScreenProjection,
    points: List<TouchPoint>,
    metrics: ScreenMetrics,
    unit: CoordinateUnit,
    paperColor: Color,
    textMeasurer: TextMeasurer,
) {
    if (points.size < 2) return

    val lastIndex = points.lastIndex
    val radius = POINT_RADIUS_DP.dp.toPx()
    val strokeWidth = 2.dp.toPx()
    for (index in 0 until lastIndex) {
        val point = points[index]
        val rank = lastIndex - index
        val pointColor = getMarkerColor(rank)
        val rimColor = getMarkerRimColor(rank)
        val center = projection.toCanvas(point.xPx, point.yPx)

        // 1. 메인 공 (단색 마커 원)
        drawCircle(
            color = pointColor,
            radius = radius,
            center = center,
        )
        // 2. 외곽 림 (아이콘 스타일 같은 계열의 연한 테두리)
        drawCircle(
            color = rimColor,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth),
        )

        // 3. 상단 (X, Y) 좌표 뱃지
        drawCoordinateBadge(
            center = center,
            radius = radius,
            point = point,
            pointColor = pointColor,
            paperColor = paperColor,
            metrics = metrics,
            unit = unit,
            textMeasurer = textMeasurer,
        )
    }
}

/**
 * 가장 최근 터치.
 * 크고 선명한 공 마커 + 상단 (X, Y) 좌표 뱃지 + 전 화면 가이드 십자선.
 */
private fun DrawScope.drawLatestPoint(
    projection: ScreenProjection,
    point: TouchPoint,
    metrics: ScreenMetrics,
    unit: CoordinateUnit,
    paperColor: Color,
    textMeasurer: TextMeasurer,
) {
    val pointColor = getMarkerColor(0)
    val rimColor = getMarkerRimColor(0)
    val center = projection.toCanvas(point.xPx, point.yPx)
    val crosshair = rimColor.copy(alpha = CROSSHAIR_ALPHA)

    // 전 화면 가이드 십자선
    drawLine(crosshair, Offset(0f, center.y), Offset(size.width, center.y), CROSSHAIR_STROKE_PX)
    drawLine(crosshair, Offset(center.x, 0f), Offset(center.x, size.height), CROSSHAIR_STROKE_PX)

    val markerRadius = MARKER_RADIUS_DP.dp.toPx()
    // 접촉 타원 크기 반영 고리
    val contactRadius = (point.touchMajorPx / 2f).coerceAtLeast(markerRadius + 8.dp.toPx())
    if (contactRadius > markerRadius) {
        drawCircle(
            color = rimColor.copy(alpha = CONTACT_ALPHA),
            radius = contactRadius,
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )
    }

    // 메인 공 (단색 마커 원)
    drawCircle(pointColor, markerRadius, center)

    // 외곽 테두리 (같은 계열 연한 테두리)
    drawCircle(
        color = rimColor,
        radius = markerRadius,
        center = center,
        style = Stroke(width = 3.6.dp.toPx()),
    )

    // 상단 (X, Y) 좌표 뱃지
    drawCoordinateBadge(
        center = center,
        radius = markerRadius,
        point = point,
        pointColor = pointColor,
        paperColor = paperColor,
        metrics = metrics,
        unit = unit,
        textMeasurer = textMeasurer,
    )
}

/** 점(공) 상단에 뜨는 실시간 (X, Y) 좌표 뱃지 */
private fun DrawScope.drawCoordinateBadge(
    center: Offset,
    radius: Float,
    point: TouchPoint,
    pointColor: Color,
    paperColor: Color,
    metrics: ScreenMetrics,
    unit: CoordinateUnit,
    textMeasurer: TextMeasurer,
) {
    val coordText = "(${CoordinateFormat.x(point, metrics, unit)}, ${CoordinateFormat.y(point, metrics, unit)})"
    val textLayoutResult = textMeasurer.measure(
        text = coordText,
        style = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = pointColor,
        ),
    )
    val tw = textLayoutResult.size.width.toFloat()
    val th = textLayoutResult.size.height.toFloat()
    val padX = 5.dp.toPx()
    val padY = 2.dp.toPx()
    val pillW = tw + padX * 2f
    val pillH = th + padY * 2f

    val gap = 6.dp.toPx()
    val margin = 8.dp.toPx()

    val leftIfCentered = center.x - pillW / 2f
    val rightIfCentered = center.x + pillW / 2f
    val topIfAbove = center.y - radius - pillH - gap
    val bottomIfBelow = center.y + radius + gap + pillH

    // 가로(X): 좌우 경계 침범 시 공의 반대편(오른쪽 또는 왼쪽)으로 동적 전환
    val rawX = when {
        leftIfCentered < margin -> center.x + radius + gap
        rightIfCentered > size.width - margin -> center.x - radius - gap - pillW
        else -> leftIfCentered
    }

    // 세로(Y): 상하 경계 침범 시 공의 반대편(아래 또는 위)으로 동적 전환
    val rawY = when {
        topIfAbove < margin -> center.y + radius + gap
        bottomIfBelow > size.height - margin -> center.y - radius - pillH - gap
        else -> topIfAbove
    }

    val labelX = rawX.coerceIn(margin, (size.width - pillW - margin).coerceAtLeast(margin))
    val labelY = rawY.coerceIn(margin, (size.height - pillH - margin).coerceAtLeast(margin))

    // 뱃지 배경 (순백색)
    drawRoundRect(
        color = paperColor,
        topLeft = Offset(labelX, labelY),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
    )
    // 뱃지 테두리
    drawRoundRect(
        color = pointColor,
        topLeft = Offset(labelX, labelY),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1.dp.toPx()),
    )
    // 텍스트 출력
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(labelX + padX, labelY + padY),
    )
}

/** 모눈종이 우측 하단에 화면 기준 해상도(치수)를 은은하게 표시 */
private fun DrawScope.drawResolutionReference(
    metrics: ScreenMetrics,
    unit: CoordinateUnit,
    color: Color,
    textMeasurer: TextMeasurer,
) {
    val text = CoordinateFormat.referenceLabel(unit, metrics)
    val textLayoutResult = textMeasurer.measure(
        text = text,
        style = TextStyle(
            fontSize = 11.sp,
            color = color.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium,
        ),
    )
    val marginX = 14.dp.toPx()
    val marginY = 12.dp.toPx()
    val x = size.width - textLayoutResult.size.width - marginX
    val y = size.height - textLayoutResult.size.height - marginY
    if (x >= 0 && y >= 0) {
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(x, y),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun GraphPaperTabPreview() {
    TouchTrackerTheme {
        GraphPaperTab(state = CaptureUiState(livePoints = previewTouchPoints()))
    }
}

private const val EMPTY_RECORDING = "터치하여 좌표 기록 중"
private const val EMPTY_IDLE = "화면을 터치하여 좌표를 측정하세요"

private const val MIN_VISIBLE_STEP_PX = 4f
private const val HALF_PX = 0.5f

private const val GRID_STROKE_PX = 1f
private const val TRAIL_STROKE_PX = 3f
private const val CROSSHAIR_STROKE_PX = 2f

private const val POINT_RADIUS_DP = 10
private const val MARKER_RADIUS_DP = 18

private const val TRAIL_ALPHA = 0.5f
private const val CROSSHAIR_ALPHA = 0.65f
private const val CONTACT_ALPHA = 0.4f

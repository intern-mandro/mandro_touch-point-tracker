package com.mandro.touchtracker.ui.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mandro.touchtracker.core.geometry.ScreenMetrics
import com.mandro.touchtracker.model.CaptureSettings
import com.mandro.touchtracker.model.CoordinateUnit
import com.mandro.touchtracker.model.TouchPoint
import com.mandro.touchtracker.ui.components.SectionCard
import com.mandro.touchtracker.ui.theme.LocalTrackerColors
import com.mandro.touchtracker.ui.theme.NumericLargeTextStyle
import com.mandro.touchtracker.ui.theme.NumericSmallTextStyle
import com.mandro.touchtracker.ui.theme.TouchTrackerTheme

/**
 * 탭 ①: 최근 [CaptureSettings.RECENT_POINT_LIMIT] 개를 숫자로.
 *
 * 맨 위에 방금 찍힌 좌표를 크게 한 번 더 보여준다. 실험 중에는 보통 마지막 한 점만
 * 읽고 받아 적기 때문에, 그걸 목록에서 눈으로 찾게 하지 않는다.
 *
 * 최근 9개만 보여주는 건 의도된 제약이다. 전체 이력은 세션 상세 화면에서 본다.
 */
@Composable
fun TouchDataTab(
    state: CaptureUiState,
    modifier: Modifier = Modifier,
) {
    val unit = state.settings.coordinateUnit
    val unitLabel = CoordinateFormat.unitLabel(unit, state.metrics)

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = SIDE_PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LatestReadout(state, unitLabel, Modifier.padding(top = 12.dp))

        SectionCard(
            title = "최근 ${CaptureSettings.RECENT_POINT_LIMIT}개",
            modifier = Modifier.fillMaxWidth(),
            trailing = { Text(unitLabel, style = NumericSmallTextStyle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        ) {
            if (state.recentPoints.isEmpty()) {
                Text(
                    text = "아직 기록된 터치가 없습니다",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                PointTable(state.recentPoints, state.metrics, unit)
            }
        }
    }
}

/** 방금 찍힌 좌표. 멀리서도 읽히도록 이 화면에서 가장 큰 글자다. */
@Composable
private fun LatestReadout(
    state: CaptureUiState,
    unitLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTrackerColors.current
    val point = state.latestPoint

    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            AxisReadout("X", point?.let { CoordinateFormat.x(it, state.metrics, state.settings.coordinateUnit) }, unitLabel)
            AxisReadout("Y", point?.let { CoordinateFormat.y(it, state.metrics, state.settings.coordinateUnit) }, unitLabel)
        }
        if (point != null) {
            Text(
                text = point.phase.name,
                style = MaterialTheme.typography.labelSmall,
                color = colors.marker,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun AxisReadout(axis: String, value: String?, unitLabel: String) {
    Column {
        Text(
            text = axis,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value ?: PLACEHOLDER,
                style = NumericLargeTextStyle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (unitLabel.isNotEmpty()) {
                Text(
                    text = unitLabel,
                    style = NumericSmallTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 3.dp, bottom = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun PointTable(
    points: List<TouchPoint>,
    metrics: ScreenMetrics,
    unit: CoordinateUnit,
) {
    Column {
        TableRow(
            index = "#",
            x = "X",
            y = "Y",
            pressure = "압력",
            elapsed = "시각",
            isHeader = true,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // 행이 9개로 고정이라 LazyColumn 을 쓰지 않는다 — 지연 로딩의 이점은 없고
        // 스크롤 컨테이너가 하나 더 생기면 부모 Column 안에서 높이 계산만 꼬인다.
        for (point in points) {
            TableRow(
                index = "${point.sequence}",
                x = CoordinateFormat.x(point, metrics, unit),
                y = CoordinateFormat.y(point, metrics, unit),
                pressure = CoordinateFormat.pressure(point.pressure),
                elapsed = CoordinateFormat.elapsed(point.elapsedMs),
            )
        }
    }
}

@Composable
private fun TableRow(
    index: String,
    x: String,
    y: String,
    pressure: String,
    elapsed: String,
    isHeader: Boolean = false,
) {
    val color = if (isHeader) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val style = if (isHeader) MaterialTheme.typography.labelSmall else NumericSmallTextStyle

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = ROW_PADDING_DP.dp),
    ) {
        Text(text = index, style = style, color = color, modifier = Modifier.width(INDEX_WIDTH_DP.dp))
        Cell(text = x, style = style, color = color, weight = 1f)
        Cell(text = y, style = style, color = color, weight = 1f)
        Cell(text = pressure, style = style, color = color, weight = 0.8f)
        Cell(text = elapsed, style = style, color = color, weight = 1f)
    }
}

/** 숫자 칸. 오른쪽 정렬이라 자릿수가 달라도 소수점이 세로로 맞는다. */
@Composable
private fun RowScope.Cell(text: String, style: TextStyle, color: Color, weight: Float) {
    Text(
        text = text,
        style = style,
        color = color,
        textAlign = TextAlign.End,
        modifier = Modifier.weight(weight),
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun TouchDataTabPreview() {
    TouchTrackerTheme {
        TouchDataTab(state = CaptureUiState(isRecording = true, livePoints = previewTouchPoints()))
    }
}

private const val PLACEHOLDER = "—"
private const val SIDE_PADDING_DP = 16
private const val ROW_PADDING_DP = 5
private const val INDEX_WIDTH_DP = 34

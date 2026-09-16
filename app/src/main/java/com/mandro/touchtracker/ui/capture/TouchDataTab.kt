package com.mandro.touchtracker.ui.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.mandro.touchtracker.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandro.touchtracker.core.geometry.ScreenMetrics
import com.mandro.touchtracker.model.CaptureSettings
import com.mandro.touchtracker.model.CoordinateUnit
import com.mandro.touchtracker.model.TouchPoint
import com.mandro.touchtracker.ui.theme.NumericSmallTextStyle
import com.mandro.touchtracker.ui.theme.TouchTrackerTheme
import com.mandro.touchtracker.ui.theme.getMarkerColor
import com.mandro.touchtracker.ui.theme.getMarkerRimColor

/**
 * 탭 ①: 최근 [CaptureSettings.RECENT_POINT_LIMIT] 개를 표 형태로 표시.
 *
 * 불필요한 상단 좌표 표기를 제거하고, 최근 포인트 목록 표를 깔끔하게 보여준다.
 */
@Composable
fun TouchDataTab(
    state: CaptureUiState,
    modifier: Modifier = Modifier,
    onGoToCanvas: () -> Unit = {},
) {
    val unit = state.settings.coordinateUnit
    val referenceLabel = CoordinateFormat.referenceLabel(unit, state.metrics)

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = SIDE_PADDING_DP.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 초기화는 상단바로 옮겼다. 두 탭이 같은 버튼 하나를 쓴다.
                Spacer(Modifier.weight(1f))
                Text(
                    text = referenceLabel,
                    style = NumericSmallTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 데이터가 없어도 표는 그대로 둔다. 빈 화면에 문구만 띄우면 측정 직전에
            // 표가 나타났다 사라져 화면이 들썩이고, 어느 칸에 무엇이 찍힐지도 안 보인다.
            PointTable(state.recentPoints, state.metrics, unit)
        }

        // 아직 아무것도 안 들어왔을 때. 표의 뼈대는 비쳐 보이게 두고 한 겹 덮어
        // "지금은 읽을 게 없다" 를 알린다. 표를 아예 숨기지 않는 이유는 위와 같다.
        if (!state.hasPoints) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    // 테마와 무관하게 어둡게 깔아야 한다. onBackground 를 쓰면
                    // 다크 테마에서 도리어 밝아진다.
                    .background(Color.Black.copy(alpha = EMPTY_SCRIM_ALPHA)),
                // 표 한가운데가 아니라 위쪽에 띄운다. 가운데에 두면 빈 행을 위아래로
                // 반씩 가려서, 표가 몇 줄짜리인지조차 안 보인다.
                contentAlignment = Alignment.TopCenter,
            ) {
                EmptyStateCard(
                    onGoToCanvas = onGoToCanvas,
                    modifier = Modifier
                        .padding(top = EMPTY_CARD_TOP_DP.dp)
                        .fillMaxWidth(EMPTY_CARD_WIDTH_FRACTION),
                )
            }
        }
    }
}

/**
 * 빈 상태 안내 상자.
 *
 * 문구만 가운데 띄우면 표의 행 사이에 걸쳐 읽기 사납다. 표 위에 떠 있는 판으로
 * 만들어 글자가 격자에서 분리되게 하고, 바로 다음 행동(캔버스로 이동)을 같이 준다.
 */
@Composable
private fun EmptyStateCard(onGoToCanvas: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(EMPTY_CARD_CORNER_DP.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = EMPTY_CARD_PADDING_DP.dp,
                vertical = EMPTY_CARD_PADDING_DP.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.capture_no_points),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Button(
                onClick = onGoToCanvas,
                contentPadding = PaddingValues(
                    horizontal = EMPTY_BUTTON_H_PADDING_DP.dp,
                    vertical = 0.dp,
                ),
                modifier = Modifier.height(EMPTY_BUTTON_HEIGHT_DP.dp),
            ) {
                Text(
                    text = stringResource(R.string.capture_go_record),
                    style = MaterialTheme.typography.labelSmall,
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
    val outlineColor = MaterialTheme.colorScheme.outline
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = outlineColor.copy(alpha = 0.5f)
    val rowDividerColor = outlineColor.copy(alpha = 0.25f)

    Column(modifier = Modifier.fillMaxWidth()) {
        // 테이블 컬럼 헤더 (Mark7 모터 상태 스타일: #, X, Y, 시각 각 구역 중앙 정렬)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "index",
                style = MaterialTheme.typography.labelSmall,
                color = mutedColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.8f),
            )
            // #과 좌표 수치 사이 연한 수직 구분선
            Box(
                modifier = Modifier
                    .width(0.8.dp)
                    .height(12.dp)
                    .background(dividerColor),
            )
            Text(
                text = "X",
                style = MaterialTheme.typography.labelSmall,
                color = mutedColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1.2f),
            )
            Text(
                text = "Y",
                style = MaterialTheme.typography.labelSmall,
                color = mutedColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1.2f),
            )
            Text(
                text = "시각",
                style = MaterialTheme.typography.labelSmall,
                color = mutedColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }

        HorizontalDivider(
            color = dividerColor,
            thickness = 0.8.dp,
        )

        // 행은 언제나 RECENT_POINT_LIMIT 개. 아직 안 들어온 자리는 비워 둔다.
        val rows = List(CaptureSettings.RECENT_POINT_LIMIT) { points.getOrNull(it) }

        // 데이터 행
        for ((index, point) in rows.withIndex()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 터치 순번 & 색상 뱃지 (가운데 정렬)
                Row(
                    modifier = Modifier.weight(0.8f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 빈 자리의 색 뱃지는 옅게 — 칸은 있지만 아직 값이 없다는 뜻.
                    val indicatorColor = getMarkerColor(index)
                    val indicatorRimColor = getMarkerRimColor(index)
                    val badgeAlpha = if (point != null) 1f else EMPTY_ROW_ALPHA
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(indicatorColor.copy(alpha = badgeAlpha), shape = CircleShape)
                            .border(1.2.dp, indicatorRimColor.copy(alpha = badgeAlpha), CircleShape),
                    )
                    Spacer(Modifier.width(6.dp))
                    // 이 세션에서 **몇 번째 터치**인가. 표는 최신이 맨 위라
                    // 아래로 갈수록 번호가 줄어든다 — 지금까지 몇 번 찍었는지가
                    // 맨 윗줄 숫자로 바로 읽힌다.
                    //
                    // 아직 아무것도 안 들어왔을 때만 자리 번호(1..9)로 표의 틀을 보여 준다.
                    val indexLabel = when {
                        point != null -> "${point.sequence + 1}"
                        points.isEmpty() -> "${index + 1}"
                        else -> EMPTY_CELL
                    }
                    Text(
                        text = indexLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // 수직 구분선
                Box(
                    modifier = Modifier
                        .width(0.8.dp)
                        .height(18.dp)
                        .background(dividerColor),
                )

                // X 좌표
                Text(
                    text = point?.let { CoordinateFormat.x(it, metrics, unit) } ?: EMPTY_CELL,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.2f),
                )

                // Y 좌표
                Text(
                    text = point?.let { CoordinateFormat.y(it, metrics, unit) } ?: EMPTY_CELL,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.2f),
                )

                // 시각
                Text(
                    text = point?.let { CoordinateFormat.elapsed(it.elapsedMs) } ?: EMPTY_CELL,
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }

            if (index < rows.lastIndex) {
                HorizontalDivider(
                    color = rowDividerColor,
                    thickness = 0.8.dp,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun TouchDataTabPreview() {
    TouchTrackerTheme {
        TouchDataTab(state = CaptureUiState(isRecording = true, livePoints = previewTouchPoints()))
    }
}

private const val SIDE_PADDING_DP = 16

private const val EMPTY_CELL = "-"
private const val EMPTY_ROW_ALPHA = 0.18f

private const val EMPTY_SCRIM_ALPHA = 0.07f

private const val EMPTY_CARD_CORNER_DP = 14
private const val EMPTY_CARD_PADDING_DP = 26
private const val EMPTY_CARD_TOP_DP = 118
private const val EMPTY_CARD_WIDTH_FRACTION = 0.84f
private const val EMPTY_BUTTON_HEIGHT_DP = 30
private const val EMPTY_BUTTON_H_PADDING_DP = 14

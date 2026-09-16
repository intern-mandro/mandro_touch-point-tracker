package com.mandro.touchtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mandro.touchtracker.ui.theme.LocalTrackerColors

/** 제목 + 내용 한 덩어리. 화면마다 카드 스타일을 다시 짜지 않으려고 둔다. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(SECTION_CORNER_DP.dp),
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(SECTION_PADDING_DP.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                trailing?.invoke()
            }
            content()
        }
    }
}

/**
 * 기록 중 여부를 나타내는 점.
 *
 * 색만으로 구분하지 않는다 — 옆의 [label] 텍스트가 같은 정보를 글로도 전달한다
 * (색각 이상 대응).
 */
@Composable
fun StatusDot(active: Boolean, label: String, modifier: Modifier = Modifier) {
    val colors = LocalTrackerColors.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(DOT_SIZE_DP.dp)
                .clip(CircleShape)
                .background(if (active) colors.live else MaterialTheme.colorScheme.outline),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** 경고·안내 한 줄. */
@Composable
fun HintText(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color ?: MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

private const val SECTION_CORNER_DP = 12
private const val SECTION_PADDING_DP = 14
private const val DOT_SIZE_DP = 8

package com.mandro.touchtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Material 색 슬롯에 안 들어가는 앱 고유 색. 좌표계와 마커는 이 앱의 도메인 색이지
 * primary/secondary 같은 일반 슬롯이 아니다.
 */
@Immutable
data class TrackerColors(
    val gridLine: Color,
    val gridLineMajor: Color,
    val marker: Color,
    val trace: Color,
    val live: Color,
    val alert: Color,
)

val LocalTrackerColors = staticCompositionLocalOf {
    TrackerColors(GridLine, GridLineMajor, MarkerRed, TraceAmber, LiveGreen, AlertRed)
}

private val LightScheme = lightColorScheme(
    primary = InkBlue,
    onPrimary = PaperCream,
    secondary = GridLineMajor,
    background = PaperCream,
    onBackground = InkBlue,
    surface = PaperCard,
    onSurface = InkBlue,
    onSurfaceVariant = InkBlueSoft,
    outline = GridLine,
    error = AlertRed,
)

private val DarkScheme = darkColorScheme(
    primary = BlueprintInk,
    onPrimary = BlueprintNavy,
    secondary = GridLineMajorDark,
    background = BlueprintNavy,
    onBackground = BlueprintInk,
    surface = BlueprintCard,
    onSurface = BlueprintInk,
    onSurfaceVariant = BlueprintInkSoft,
    outline = GridLineDark,
    error = AlertRed,
)

@Composable
fun TouchTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Dynamic color(Material You)는 일부러 쓰지 않는다. 격자와 마커의 대비가
    // 기기 배경화면에 따라 달라지면 측정 화면의 가독성을 보장할 수 없다.
    val trackerColors = if (darkTheme) {
        TrackerColors(GridLineDark, GridLineMajorDark, MarkerRed, TraceAmber, LiveGreen, AlertRed)
    } else {
        TrackerColors(GridLine, GridLineMajor, MarkerRed, TraceAmber, LiveGreen, AlertRed)
    }

    CompositionLocalProvider(LocalTrackerColors provides trackerColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

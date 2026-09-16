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
    /** 탭 바 바탕. 어느 탭에 있는지가 한눈에 보이도록 본문보다 진하게 깐다. */
    val tabBar: Color,
    /** 탭 바 위의 글자. 선택된 탭은 이 색 그대로, 나머지는 옅게 쓴다. */
    val onTabBar: Color,
)

val LocalTrackerColors = staticCompositionLocalOf {
    TrackerColors(
        GridLine, GridLineMajor, MarkerRed, TraceAmber, LiveGreen, AlertRed,
        tabBar = InkBlue, onTabBar = PaperCream,
    )
}

/**
 * Material 기본 스킴에는 보라(baseline purple)가 남아 있다. 특히 스낵바
 * (`inverseSurface`/`inversePrimary`)와 다이얼로그(`surfaceContainerHigh`)는
 * 앱에서 지정하지 않으면 보랏빛 회색으로 뜬다. 여기서 전 슬롯을
 * 흰색/중성 회색으로 못박아 보라 기운을 제거한다.
 */
private val LightScheme = lightColorScheme(
    primary = InkBlue,
    onPrimary = PureWhite,
    primaryContainer = SurfaceMuted,
    onPrimaryContainer = InkBlue,
    inversePrimary = InkBlue,
    secondary = GridLineMajor,
    onSecondary = InkBlue,
    secondaryContainer = SurfaceMuted,
    onSecondaryContainer = InkBlue,
    tertiary = InkBlueSoft,
    onTertiary = PureWhite,
    tertiaryContainer = SurfaceMuted,
    onTertiaryContainer = InkBlue,
    background = PaperCream,
    onBackground = InkBlue,
    surface = PaperCard,
    onSurface = InkBlue,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkBlueSoft,
    surfaceTint = PureWhite,
    surfaceBright = PureWhite,
    surfaceDim = SurfaceDim,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = PureWhite,
    surfaceContainer = PureWhite,
    surfaceContainerHigh = PureWhite,
    surfaceContainerHighest = PureWhite,
    // 스낵바 바탕/글자. 기본값(어두운 보랏빛 회색) 대신 흰 종이 + 잉크 글자.
    inverseSurface = PureWhite,
    inverseOnSurface = InkBlue,
    outline = GridLine,
    outlineVariant = GridLine,
    error = AlertRed,
    onError = PureWhite,
    errorContainer = ErrorSurface,
    onErrorContainer = AlertRed,
    scrim = Color(0xFF000000),
)

private val DarkScheme = darkColorScheme(
    primary = BlueprintInk,
    onPrimary = BlueprintNavy,
    primaryContainer = BlueprintMuted,
    onPrimaryContainer = BlueprintInk,
    inversePrimary = InkBlue,
    secondary = GridLineMajorDark,
    onSecondary = BlueprintInk,
    secondaryContainer = BlueprintMuted,
    onSecondaryContainer = BlueprintInk,
    tertiary = BlueprintInkSoft,
    onTertiary = BlueprintNavy,
    tertiaryContainer = BlueprintMuted,
    onTertiaryContainer = BlueprintInk,
    background = BlueprintNavy,
    onBackground = BlueprintInk,
    surface = BlueprintCard,
    onSurface = BlueprintInk,
    surfaceVariant = BlueprintMuted,
    onSurfaceVariant = BlueprintInkSoft,
    surfaceTint = BlueprintCard,
    surfaceBright = BlueprintMuted,
    surfaceDim = BlueprintDim,
    surfaceContainerLowest = BlueprintDim,
    surfaceContainerLow = BlueprintNavy,
    surfaceContainer = BlueprintCard,
    surfaceContainerHigh = BlueprintCard,
    surfaceContainerHighest = BlueprintMuted,
    // 다크에서도 알림은 흰 카드로 띄운다 (라이트와 같은 인상 유지).
    inverseSurface = PureWhite,
    inverseOnSurface = InkBlue,
    outline = GridLineDark,
    outlineVariant = GridLineDark,
    error = AlertRed,
    onError = PureWhite,
    errorContainer = ErrorSurfaceDark,
    onErrorContainer = MarkerRed,
    scrim = Color(0xFF000000),
)

@Composable
fun TouchTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Dynamic color(Material You)는 일부러 쓰지 않는다. 격자와 마커의 대비가
    // 기기 배경화면에 따라 달라지면 측정 화면의 가독성을 보장할 수 없다.
    val trackerColors = if (darkTheme) {
        TrackerColors(
            GridLineDark, GridLineMajorDark, MarkerRed, TraceAmber, LiveGreen, AlertRed,
            // 다크에서는 배경이 이미 어두우니 탭 바를 한 단계 띄워 구분한다.
            tabBar = BlueprintCard, onTabBar = BlueprintInk,
        )
    } else {
        TrackerColors(
            GridLine, GridLineMajor, MarkerRed, TraceAmber, LiveGreen, AlertRed,
            tabBar = InkBlue, onTabBar = PaperCream,
        )
    }

    CompositionLocalProvider(LocalTrackerColors provides trackerColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

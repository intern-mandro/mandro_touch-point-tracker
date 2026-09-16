package com.mandro.touchtracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 좌표 숫자 전용 스타일.
 *
 * 등폭(Monospace)이 필수다. 비례 폰트로 숫자를 세로로 쌓으면 자릿수가 바뀔 때마다
 * 소수점이 좌우로 흔들려서, 최근 9개를 세로로 훑으며 값을 비교하는 게 불가능해진다.
 */
val NumericTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    letterSpacing = 0.sp,
)

val NumericSmallTextStyle = NumericTextStyle.copy(fontSize = 12.sp)

val NumericLargeTextStyle = NumericTextStyle.copy(
    fontSize = 22.sp,
    fontWeight = FontWeight.Bold,
)

val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = (-0.2).sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp,
    ),
)

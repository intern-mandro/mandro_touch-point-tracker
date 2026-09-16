package com.mandro.touchtracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 색 방향: **제도용 모눈종이 + 계측 장비**.
 *
 * 참고 앱(Mark7)의 그래파이트+청록과 일부러 다르게 잡았다. 실험대 위에 두 앱이 같이
 * 떠 있을 때 한눈에 구분돼야 한다. 여기는 따뜻한 종이색 바탕에 잉크 남색 격자,
 * 그 위에 형광 주황 마커 하나 — 종이에 기록하는 물건이라는 은유를 유지한다.
 *
 * 색은 의미를 갖는다. 장식으로 쓰지 않는다:
 *  - [InkBlue]    : 좌표계 (격자·축·눈금)
 *  - [MarkerRed]  : 가장 최근 터치 1점
 *  - [TraceAmber] : 그 이전 터치들 (오래될수록 옅어짐)
 *  - [LiveGreen]  : 기록 중 상태
 */

// ── 라이트: 클린 화이트 ───────────────────────────────────────
val PureWhite = Color(0xFFFFFFFF)
val PaperCream = PureWhite
val PaperCard = PureWhite
val InkBlue = Color(0xFF16212B)
val InkBlueSoft = Color(0xFF4A5C6B)
val GridLine = Color(0xFFE4E9F0)
val GridLineMajor = Color(0xFFBAC8D5)

/** 스낵바·다이얼로그·토널 버튼처럼 "한 단계 눌린 면"에 쓰는 중성 회색. 보라 기운 없음. */
val SurfaceMuted = Color(0xFFF2F4F7)
val SurfaceDim = Color(0xFFE8EBF0)
val ErrorSurface = Color(0xFFFCE8E6)

// ── 다크: 청사진 ──────────────────────────────────────────────
val BlueprintNavy = Color(0xFF0E1620)
val BlueprintCard = Color(0xFF16212D)
val BlueprintInk = Color(0xFFE3EAF0)
val BlueprintInkSoft = Color(0xFF93A6B5)
val GridLineDark = Color(0xFF2B3E50)
val GridLineMajorDark = Color(0xFF41627C)
val BlueprintMuted = Color(0xFF1E2A38)
val BlueprintDim = Color(0xFF0A1219)
val ErrorSurfaceDark = Color(0xFF3A1A16)

// ── 의미색 (양쪽 테마 공통) ───────────────────────────────────
val MarkerRed = Color(0xFFE8452C)
val TraceAmber = Color(0xFFD98A1F)
val LiveGreen = Color(0xFF1F9D55)
val AlertRed = Color(0xFFC0392B)

// ── 9색 터치 마커 팔레트 (빨, 주, 노, 연두, 초록, 하늘, 파랑, 남색, 보라) ───────────
val MarkerPalette = listOf(
    Color(0xFFE53935), // 1. 빨강 (Red)
    Color(0xFFFF6D00), // 2. 주황 (Orange)
    Color(0xFFF9A825), // 3. 노랑 (Yellow)
    Color(0xFF7CB342), // 4. 연두 (Light Green)
    Color(0xFF1E8E3E), // 5. 초록 (Green)
    Color(0xFF00B4D8), // 6. 하늘 (Vivid Cyan Sky)
    Color(0xFF1D4ED8), // 7. 파랑 (Royal Blue)
    Color(0xFF283593), // 8. 남색 (Navy)
    Color(0xFF8E24AA), // 9. 보라 (Purple)
)

// ── 9색 터치 마커 테두리 팔레트 (아이콘 스타일: 같은 계열의 연한 색) ─────────
val MarkerRimPalette = listOf(
    Color(0xFFFFCDD2), // 1. 빨강 연한 테두리
    Color(0xFFFFE0B2), // 2. 주황 연한 테두리
    Color(0xFFFFF9C4), // 3. 노랑 연한 테두리
    Color(0xFFDCEDC8), // 4. 연두 연한 테두리
    Color(0xFFC8E6C9), // 5. 초록 연한 테두리
    Color(0xFFE0F7FA), // 6. 하늘 연한 테두리
    Color(0xFFDBEAFE), // 7. 파랑 연한 테두리
    Color(0xFFC5CAE9), // 8. 남색 연한 테두리
    Color(0xFFE1BEE7), // 9. 보라 연한 테두리
)

fun getMarkerColor(sequence: Int): Color {
    val index = (sequence % MarkerPalette.size + MarkerPalette.size) % MarkerPalette.size
    return MarkerPalette[index]
}

fun getMarkerRimColor(sequence: Int): Color {
    val index = (sequence % MarkerRimPalette.size + MarkerRimPalette.size) % MarkerRimPalette.size
    return MarkerRimPalette[index]
}


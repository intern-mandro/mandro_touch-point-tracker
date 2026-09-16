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

// ── 라이트: 제도 용지 ─────────────────────────────────────────
val PaperCream = Color(0xFFF7F3EA)
val PaperCard = Color(0xFFFFFDF8)
val InkBlue = Color(0xFF16212B)
val InkBlueSoft = Color(0xFF4A5C6B)
val GridLine = Color(0xFF9FB4C4)
val GridLineMajor = Color(0xFF5C7C93)

// ── 다크: 청사진 ──────────────────────────────────────────────
val BlueprintNavy = Color(0xFF0E1620)
val BlueprintCard = Color(0xFF16212D)
val BlueprintInk = Color(0xFFE3EAF0)
val BlueprintInkSoft = Color(0xFF93A6B5)
val GridLineDark = Color(0xFF2B3E50)
val GridLineMajorDark = Color(0xFF41627C)

// ── 의미색 (양쪽 테마 공통) ───────────────────────────────────
val MarkerRed = Color(0xFFE8452C)
val TraceAmber = Color(0xFFD98A1F)
val LiveGreen = Color(0xFF1F9D55)
val AlertRed = Color(0xFFC0392B)

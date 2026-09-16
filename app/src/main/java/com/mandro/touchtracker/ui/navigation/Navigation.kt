package com.mandro.touchtracker.ui.navigation

import androidx.annotation.StringRes
import com.mandro.touchtracker.R

sealed class Screen(val route: String) {
    /** 메인. 탭 2개(데이터·모눈종이)를 가진 캡처 화면. */
    data object Capture : Screen("capture")

    data object Sessions : Screen("sessions")

    data object Settings : Screen("settings")

    data object SessionDetail : Screen("sessions/{sessionId}") {
        const val ARG_SESSION_ID = "sessionId"
        fun createRoute(sessionId: Long) = "sessions/$sessionId"
    }
}

/**
 * 메인 화면의 탭. 둘 다 **같은 데이터를 다르게 보여줄 뿐**이다 —
 * 탭을 옮겨도 기록은 끊기지 않는다(캡처는 Activity 레벨에 있다).
 */
enum class CaptureTab(@StringRes val labelRes: Int) {
    /** 최근 N개를 숫자로. 값을 읽어 적어야 할 때. */
    DATA(R.string.tab_data),

    /** 모눈종이 위 점 분포로. 흩어짐을 눈으로 볼 때. */
    GRID(R.string.tab_grid),
}

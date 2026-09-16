package com.mandro.touchtracker.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mandro.touchtracker.model.DeviceProfile
import com.mandro.touchtracker.model.TouchSession
import com.mandro.touchtracker.ui.capture.CaptureScreen
import com.mandro.touchtracker.ui.detail.SessionDetailScreen
import com.mandro.touchtracker.ui.navigation.Screen
import com.mandro.touchtracker.ui.sessions.SessionListScreen
import com.mandro.touchtracker.ui.settings.SettingsScreen

/**
 * 앱의 화면 그래프.
 *
 * [Screen.Capture] 가 항상 백스택 바닥이다 — 실험 중에는 캡처 화면으로 한 번에
 * 돌아올 수 있어야 하고, 다른 화면은 잠깐 들렀다 나오는 곳이다.
 */
@Composable
fun TouchTrackerApp(
    isRecording: Boolean,
    deviceProfile: () -> DeviceProfile,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Capture.route) {
        composable(Screen.Capture.route) {
            CaptureScreen(
                deviceProfile = deviceProfile,
                onOpenSessions = { navController.navigate(Screen.Sessions.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.Sessions.route) {
            SessionListScreen(
                onBack = navController::popBackStack,
                onOpenSession = { sessionId ->
                    navController.navigate(Screen.SessionDetail.createRoute(sessionId))
                },
            )
        }

        composable(
            route = Screen.SessionDetail.route,
            arguments = listOf(
                navArgument(Screen.SessionDetail.ARG_SESSION_ID) {
                    type = NavType.LongType
                    defaultValue = TouchSession.NO_ID
                },
            ),
        ) {
            SessionDetailScreen(onBack = navController::popBackStack)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                isRecording = isRecording,
                onBack = navController::popBackStack,
            )
        }
    }
}

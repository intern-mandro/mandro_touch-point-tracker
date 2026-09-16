package com.mandro.touchtracker

import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mandro.touchtracker.data.local.DeviceProfileProvider
import com.mandro.touchtracker.data.touch.TouchCaptureController
import com.mandro.touchtracker.data.local.SettingsRepository
import com.mandro.touchtracker.ui.TouchTrackerApp
import com.mandro.touchtracker.ui.theme.TouchTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 앱의 유일한 Activity이자 **터치 캡처 지점**.
 *
 * 좌표 수집을 Composable 이 아니라 여기서 하는 이유는 [TouchCaptureController] 문서 참고.
 * 요약하면: 어느 탭이 떠 있든 로봇 손의 터치는 빠짐없이 기록돼야 하기 때문이다.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var captureController: TouchCaptureController
    @Inject lateinit var deviceProfileProvider: DeviceProfileProvider
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeKeepScreenOn()

        setContent {
            TouchTrackerTheme {
                val activeSession by captureController.activeSession.collectAsStateWithLifecycle()
                TouchTrackerApp(
                    isRecording = activeSession != null,
                    // 세션 좌표계는 "지금 이 Activity 창" 기준이어야 한다.
                    // Application Context 로 재면 시스템 바가 섞여 원점이 어긋난다.
                    deviceProfile = { deviceProfileProvider.current(this) },
                )
            }
        }
    }

    /**
     * 모든 터치를 **관찰만** 하고 그대로 흘려보낸다.
     *
     * 여기서 이벤트를 소비하면 탭 전환·버튼이 죽는다. 반환값은 항상 super 의 결과다.
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        captureController.onMotionEvent(event)
        return super.dispatchTouchEvent(event)
    }

    /** 로봇 팔이 정렬을 마칠 때까지 화면이 꺼지면 측정이 끊긴다. */
    private fun observeKeepScreenOn() {
        lifecycleScope.launch {
            settingsRepository.settings
                .map { it.keepScreenOn }
                .collect { keepOn ->
                    if (keepOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
        }
    }
}

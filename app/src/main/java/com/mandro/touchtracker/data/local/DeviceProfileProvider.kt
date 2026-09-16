package com.mandro.touchtracker.data.local

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.mandro.touchtracker.core.geometry.ScreenMetrics
import com.mandro.touchtracker.model.DeviceProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 지금 이 기기·화면의 좌표계를 읽는다. 세션을 열 때 한 번 호출된다.
 *
 * ApplicationContext 가 아니라 **Activity Context** 를 넘겨야 실제 창 크기가 나온다.
 * ApplicationContext 로 얻은 DisplayMetrics 는 시스템 바를 포함한 값이라
 * 터치 좌표 원점과 어긋난다.
 */
@Singleton
class DeviceProfileProvider @Inject constructor() {

    /**
     * @param activityContext 캡처 화면을 띄운 Activity. 여기서 창 크기를 읽는다.
     */
    fun current(activityContext: Context): DeviceProfile = DeviceProfile(
        model = Build.MODEL,
        manufacturer = Build.MANUFACTURER,
        androidSdk = Build.VERSION.SDK_INT,
        metrics = screenMetrics(activityContext),
    )

    private fun screenMetrics(activityContext: Context): ScreenMetrics {
        val display = activityContext.resources.displayMetrics
        val (widthPx, heightPx) = windowSize(activityContext, display)
        return ScreenMetrics(
            widthPx = widthPx,
            heightPx = heightPx,
            // xdpi/ydpi 는 제조사가 대충 채워 넣는 기기가 있다. 0 이하면
            // ScreenMetrics 가 mm 환산을 포기하고 px 를 그대로 쓴다.
            xDpi = display.xdpi,
            yDpi = display.ydpi,
            density = display.density,
        )
    }

    @Suppress("DEPRECATION")
    private fun windowSize(context: Context, fallback: DisplayMetrics): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = context.getSystemService(WindowManager::class.java) ?: return fallbackSize(fallback)
            val bounds = wm.currentWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        return fallbackSize(fallback)
    }

    private fun fallbackSize(metrics: DisplayMetrics) = metrics.widthPixels to metrics.heightPixels
}

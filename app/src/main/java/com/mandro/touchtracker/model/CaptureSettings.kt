package com.mandro.touchtracker.model

/** 화면에 좌표를 어떤 단위로 띄울지. 저장되는 원본은 언제나 px 다. */
enum class CoordinateUnit { PX, MM, NORMALIZED }

/**
 * 사람이 바꾸는 캡처/표시 설정. DataStore 에 영속화된다.
 *
 * @param gridSpacingMm      모눈종이 한 칸의 실제 물리 크기 (mm).
 * @param recordMoveEvents   false 면 DOWN/UP 만 기록한다. 조준 정확도만 볼 때는
 *                           MOVE 수천 개가 노이즈라 기본값이 false 다.
 * @param ignoreSyntheticInput  사람 손가락/에뮬레이터 주입 입력을 버릴지.
 *                           실측 데이터에 사람 손이 섞이는 사고를 막는다.
 * @param liveBufferSize     화면에 동시에 그려 둘 최근 점 개수. 렌더 비용 상한.
 */
data class CaptureSettings(
    val gridSpacingMm: Float = DEFAULT_GRID_SPACING_MM,
    val coordinateUnit: CoordinateUnit = CoordinateUnit.PX,
    val showTrail: Boolean = true,
    val keepScreenOn: Boolean = true,
    val recordMoveEvents: Boolean = false,
    val ignoreSyntheticInput: Boolean = true,
    val liveBufferSize: Int = DEFAULT_LIVE_BUFFER_SIZE,
) {
    companion object {
        const val DEFAULT_GRID_SPACING_MM = 5f
        const val MIN_GRID_SPACING_MM = 1f
        const val MAX_GRID_SPACING_MM = 20f

        const val DEFAULT_LIVE_BUFFER_SIZE = 500

        /** 데이터 탭이 텍스트로 보여주는 최근 터치 개수. */
        const val RECENT_POINT_LIMIT = 9

        val DEFAULT = CaptureSettings()
    }
}

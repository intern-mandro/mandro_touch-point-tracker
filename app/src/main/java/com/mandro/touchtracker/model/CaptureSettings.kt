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
 * @param liveBufferSize     메모리에 들고 있을 최근 점 개수. 화면에 그려지는 개수가
 *                           아니라 **저장 가능한 상한**이다 — 자세한 건 상수 설명 참고.
 */
data class CaptureSettings(
    val gridSpacingMm: Float = FIXED_GRID_SPACING_MM,
    val coordinateUnit: CoordinateUnit = CoordinateUnit.PX,
    val showTrail: Boolean = true,
    val keepScreenOn: Boolean = true,
    val recordMoveEvents: Boolean = false,
    val ignoreSyntheticInput: Boolean = true,
    val liveBufferSize: Int = DEFAULT_LIVE_BUFFER_SIZE,
) {
    companion object {
        const val FIXED_GRID_SPACING_MM = 12f
        const val DEFAULT_GRID_SPACING_MM = 12f
        const val MIN_GRID_SPACING_MM = 1f
        const val MAX_GRID_SPACING_MM = 20f

        /**
         * 메모리에 들고 있을 최근 점 개수. **표시 개수와 다르다.**
         *
         * 기록 중이 아닐 때는 이 버퍼가 측정값의 유일한 사본이라, 여기서 밀려난
         * 점은 "저장 후 초기화" 로도 되살릴 수 없다. 한때 이 값이 표시 개수와 같은
         * 9 였는데, 그 바람에 기록을 켜지 않고 잰 측정이 마지막 9 점만 저장됐다.
         * 화면 표시는 [RECENT_POINT_LIMIT] 이 따로 자르므로 여기를 키워도
         * 그려지는 점 수는 그대로다.
         */
        const val DEFAULT_LIVE_BUFFER_SIZE = 500

        /** 데이터 탭 및 모눈종이 화면에 동시에 보여줄 최근 터치 개수 (FIFO 큐). */
        const val RECENT_POINT_LIMIT = 9

        val DEFAULT = CaptureSettings()
    }
}

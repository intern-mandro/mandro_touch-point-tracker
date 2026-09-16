package com.mandro.touchtracker.data.export

import android.net.Uri
import com.mandro.touchtracker.core.AppResult
import com.mandro.touchtracker.model.ExportFormat

/**
 * 세션을 사용자가 SAF 로 고른 위치에 쓴다.
 *
 * 쓸 위치를 앱이 정하지 않는 게 핵심이다 — 실험 데이터는 보통 바로 PC 로 옮기므로
 * 사용자가 고른 폴더/파일에 직접 쓰고, 앱 내부 저장소에 사본을 남기지 않는다.
 */
interface SessionExporter {

    /** 형식별 기본 파일명. 예: `touch_20260916_143012_grip-A.csv` */
    suspend fun suggestFileName(sessionId: Long, format: ExportFormat): String

    /**
     * @param target ACTION_CREATE_DOCUMENT 로 받은 쓰기 가능한 URI
     * @return 성공 시 사람이 읽을 파일 설명, 실패 시 UI 에 띄울 메시지
     */
    suspend fun export(sessionId: Long, format: ExportFormat, target: Uri): AppResult<String>
}

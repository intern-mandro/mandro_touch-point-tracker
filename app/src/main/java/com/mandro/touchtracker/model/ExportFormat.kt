package com.mandro.touchtracker.model

/**
 * 세션 내보내기 형식.
 *
 * - [CSV]  : 엑셀·파이썬에서 바로 여는 용도. 한 줄 = 터치 1점, 헤더에 단위 포함.
 * - [JSON] : 세션 메타(기기·좌표계)까지 통째로. 재현·재생용.
 */
enum class ExportFormat(val extension: String, val mimeType: String) {
    CSV("csv", "text/csv"),
    JSON("json", "application/json"),
}

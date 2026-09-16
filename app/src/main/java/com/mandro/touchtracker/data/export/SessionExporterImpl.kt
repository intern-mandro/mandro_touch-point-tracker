package com.mandro.touchtracker.data.export

import android.content.ContentResolver
import android.net.Uri
import com.mandro.touchtracker.core.AppResult
import com.mandro.touchtracker.data.db.TouchPointDao
import com.mandro.touchtracker.data.db.entity.toDomain
import com.mandro.touchtracker.di.IoDispatcher
import com.mandro.touchtracker.data.local.SettingsRepository
import com.mandro.touchtracker.model.CoordinateUnit
import com.mandro.touchtracker.model.ExportFormat
import com.mandro.touchtracker.model.TouchSession
import com.mandro.touchtracker.data.export.SessionExporter
import com.mandro.touchtracker.data.repository.TouchSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class SessionExporterImpl @Inject constructor(
    private val sessionRepository: TouchSessionRepository,
    private val pointDao: TouchPointDao,
    private val contentResolver: ContentResolver,
    private val settingsRepository: SettingsRepository,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SessionExporter {

    override suspend fun suggestFileName(sessionId: Long, format: ExportFormat): String =
        withContext(io) {
            val session = sessionRepository.getSession(sessionId)
            val stamp = SimpleDateFormat(FILE_STAMP_PATTERN, Locale.US)
                .format(Date(session?.startedAtEpochMs ?: System.currentTimeMillis()))
            val slug = (session?.name ?: "session").sanitizeForFileName()
            "touch_${stamp}_$slug.${format.extension}"
        }

    override suspend fun export(
        sessionId: Long,
        format: ExportFormat,
        target: Uri,
    ): AppResult<String> = withContext(io) {
        val session = sessionRepository.getSession(sessionId)
            ?: return@withContext AppResult.Failure("세션을 찾을 수 없습니다 (id=$sessionId)")

        // CSV 의 좌표 단위는 "지금 설정값" 을 따른다. 내보내기를 누른 시점에 한 번
        // 읽어서 파일 하나가 통째로 같은 단위를 쓰게 한다.
        val coordinateUnit = settingsRepository.settings.first().coordinateUnit

        try {
            val count = writeSession(session, format, target, coordinateUnit)
            AppResult.Success("${session.name} · ${count}점")
        } catch (e: CancellationException) {
            // 취소는 실패가 아니다. 구조적 동시성을 깨지 않으려면 반드시 다시 던진다.
            throw e
        } catch (e: IOException) {
            AppResult.Failure("파일을 쓰지 못했습니다: ${e.message}", e)
        } catch (e: IllegalStateException) {
            AppResult.Failure("내보내기를 시작하지 못했습니다: ${e.message}", e)
        }
    }

    /** @return 실제로 쓴 점 개수 */
    private suspend fun writeSession(
        session: TouchSession,
        format: ExportFormat,
        target: Uri,
        coordinateUnit: CoordinateUnit,
    ): Int {
        // 내보내기 1회분마다 새로 만든다. JSON writer 는 배열 구분자 때문에 상태를
        // 들고 있어서, 인스턴스를 재사용하면 동시 내보내기에서 깨진다.
        val writer = when (format) {
            ExportFormat.CSV -> CsvSessionWriter(coordinateUnit)
            ExportFormat.JSON -> JsonSessionWriter(json)
        }

        // "wt" = 기존 내용을 지우고 쓰기. "w" 만 주면 기기에 따라 앞부분만 덮어써서
        // 이전 파일의 꼬리가 남는 사고가 난다.
        val stream = contentResolver.openOutputStream(target, "wt")
            ?: error("쓰기 스트림을 열 수 없습니다")

        var written = 0
        BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { out ->
            writer.begin(out, session)
            var offset = 0
            while (true) {
                coroutineContext.ensureActive()   // 사용자가 취소하면 즉시 멈춘다
                val page = pointDao.getPage(session.id, PAGE_SIZE, offset)
                if (page.isEmpty()) break
                writer.writeChunk(out, session, page.map { it.toDomain() })
                written += page.size
                offset += page.size
            }
            writer.end(out)
        }
        return written
    }
    private fun String.sanitizeForFileName(): String =
        trim().replace(INVALID_FILE_NAME_CHARS, "-")
            .take(MAX_SLUG_LENGTH)
            .ifBlank { "session" }

    private companion object {
        const val PAGE_SIZE = 2_000
        const val MAX_SLUG_LENGTH = 40
        const val FILE_STAMP_PATTERN = "yyyyMMdd_HHmmss"
        val INVALID_FILE_NAME_CHARS = Regex("""[^\p{L}\p{N}._-]+""")
    }
}

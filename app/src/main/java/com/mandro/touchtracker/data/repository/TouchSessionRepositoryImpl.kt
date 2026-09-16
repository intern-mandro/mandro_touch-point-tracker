package com.mandro.touchtracker.data.repository

import com.mandro.touchtracker.core.time.Clock
import com.mandro.touchtracker.data.db.SessionDao
import com.mandro.touchtracker.data.db.TouchPointDao
import com.mandro.touchtracker.data.db.entity.SessionEntity
import com.mandro.touchtracker.data.db.entity.toDomain
import com.mandro.touchtracker.data.db.entity.toEntity
import com.mandro.touchtracker.di.IoDispatcher
import com.mandro.touchtracker.model.DeviceProfile
import com.mandro.touchtracker.model.TouchPoint
import com.mandro.touchtracker.model.TouchSession
import com.mandro.touchtracker.data.repository.TouchSessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TouchSessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val pointDao: TouchPointDao,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : TouchSessionRepository {

    override fun observeSessions(): Flow<List<TouchSession>> =
        sessionDao.observeAll()
            .map { rows -> rows.map { it.session.toDomain(it.pointCount) } }
            .flowOn(io)

    override fun observeSession(sessionId: Long): Flow<TouchSession?> =
        sessionDao.observeById(sessionId)
            .map { row -> row?.let { it.session.toDomain(it.pointCount) } }
            .flowOn(io)

    override fun observePoints(sessionId: Long): Flow<List<TouchPoint>> =
        pointDao.observeBySession(sessionId)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(io)

    override suspend fun getSession(sessionId: Long): TouchSession? = withContext(io) {
        sessionDao.getById(sessionId)?.toDomain(pointDao.countBySession(sessionId))
    }

    override suspend fun startSession(name: String, note: String, device: DeviceProfile): Long =
        withContext(io) {
            sessionDao.insert(
                SessionEntity(
                    name = name,
                    note = note,
                    startedAtEpochMs = clock.epochMs(),
                    endedAtEpochMs = null,
                    deviceModel = device.model,
                    deviceManufacturer = device.manufacturer,
                    androidSdk = device.androidSdk,
                    widthPx = device.metrics.widthPx,
                    heightPx = device.metrics.heightPx,
                    xDpi = device.metrics.xDpi,
                    yDpi = device.metrics.yDpi,
                    density = device.metrics.density,
                ),
            )
        }

    override suspend fun endSession(sessionId: Long) = withContext(io) {
        // ended_at IS NULL 조건이 쿼리에 있어서 두 번 불러도 처음 시각이 유지된다.
        sessionDao.markEnded(sessionId, clock.epochMs())
    }

    override suspend fun appendPoints(points: List<TouchPoint>) = withContext(io) {
        if (points.isEmpty()) return@withContext
        pointDao.insertAll(points.map { it.toEntity() })
    }

    override suspend fun renameSession(sessionId: Long, name: String, note: String) =
        withContext(io) { sessionDao.rename(sessionId, name, note) }

    override suspend fun deleteSession(sessionId: Long) =
        withContext(io) { sessionDao.delete(sessionId) }
}

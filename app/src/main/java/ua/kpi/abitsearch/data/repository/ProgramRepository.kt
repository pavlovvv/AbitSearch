package ua.kpi.abitsearch.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ua.kpi.abitsearch.data.local.ProgramDao
import ua.kpi.abitsearch.data.local.SpecialtyDao
import ua.kpi.abitsearch.data.local.SyncMetaDao
import ua.kpi.abitsearch.data.local.SyncMetaEntity
import ua.kpi.abitsearch.data.remote.abit.AbitApi
import ua.kpi.abitsearch.data.toDomain
import ua.kpi.abitsearch.data.toEntity
import ua.kpi.abitsearch.domain.model.EducationLevel
import ua.kpi.abitsearch.domain.model.Program
import ua.kpi.abitsearch.domain.model.Specialty
import ua.kpi.abitsearch.domain.model.YearStat

/**
 * Освітні програми, статистика зарахування та спеціальності.
 * Та сама схема, що й для університетів: читання з Room, оновлення з сервера за TTL.
 */
class ProgramRepository(
    private val api: AbitApi,
    private val programs: ProgramDao,
    private val specialties: SpecialtyDao,
    private val meta: SyncMetaDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    // ---------- Читання (Flow з Room) ----------

    fun observeByUniversity(universityId: Long, level: EducationLevel, minYear: Int, query: String): Flow<List<Program>> =
        programs.observeByUniversity(universityId, level, minYear, query.trim().lowercase()).map { l -> l.map { it.toDomain() } }

    fun observeLatestYear(universityId: Long): Flow<Int?> = programs.observeLatestYear(universityId)

    fun observeProgram(id: Long): Flow<Program?> = programs.observeById(id).map { it?.toDomain() }

    fun observeStats(programId: Long): Flow<List<YearStat>> = programs.observeStats(programId).map { l -> l.map { it.toDomain() } }

    fun observeSpecialties(level: EducationLevel, query: String): Flow<List<Specialty>> =
        specialties.observe(level, query.trim().lowercase()).map { l -> l.map { it.toDomain() }.sortedWith(compareBy(UkrainianOrder) { it.name }) }

    fun observeSpecialty(key: String, level: EducationLevel): Flow<Specialty?> =
        specialties.observeOne(key, level).map { it?.toDomain() }

    fun observeBySpecialty(key: String, level: EducationLevel, year: Int): Flow<List<Program>> =
        programs.observeBySpecialty(key, level, year).map { l -> l.map { it.toDomain() } }

    // ---------- Оновлення з сервера ----------

    suspend fun refreshUniversity(universityId: Long, force: Boolean = false): RefreshResult =
        refresh("programs:u:$universityId", force, hasCache = { programs.countByUniversity(universityId) > 0 }) {
            val now = clock()
            programs.upsertKeepingLicense(api.programsOfUniversity(universityId).map { it.toEntity(now) })
        }

    /** Детальна інформація: програма, статистика за всі роки, ліцензія. */
    suspend fun refreshProgram(id: Long, force: Boolean = false): RefreshResult =
        refresh("program:$id", force, hasCache = { programs.statsCount(id) > 0 }) {
            val detail = api.program(id)
            programs.replaceDetail(
                detail.program.toEntity(clock(), detail.license),
                detail.stats.orEmpty().map { it.toEntity(id) },
            )
        }

    suspend fun refreshSpecialties(level: EducationLevel, force: Boolean = false): RefreshResult =
        refresh("specialties:${level.name}", force, hasCache = { specialties.count(level) > 0 }) {
            val items = api.specialties(level.name).map { it.toEntity() }
            if (items.isNotEmpty()) specialties.replaceLevel(level, items)
        }

    suspend fun refreshSpecialtyPrograms(key: String, level: EducationLevel, force: Boolean = false): RefreshResult =
        refresh("programs:s:${level.name}:$key", force, hasCache = { programs.countBySpecialty(key, level) > 0 }) {
            val now = clock()
            programs.upsertKeepingLicense(api.programsOfSpecialty(key, level.name).map { it.toEntity(now) })
        }

    /** Спільна логіка TTL: пропустити, якщо кеш свіжий; інакше завантажити й записати час оновлення. */
    private suspend fun refresh(
        key: String,
        force: Boolean,
        hasCache: suspend () -> Boolean,
        load: suspend () -> Unit,
    ): RefreshResult {
        val now = clock()
        if (!force && now - (meta.updatedAt(key) ?: 0L) < TTL_MS) return RefreshResult.Skipped
        return try {
            load()
            meta.put(SyncMetaEntity(key, now))
            RefreshResult.Success
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            RefreshResult.Error(e.toUserMessage(), hasCache = hasCache())
        }
    }

    companion object {
        /** Статистика зарахування - річні дані; кеш на 12 годин. */
        const val TTL_MS = 12L * 60 * 60 * 1000
    }
}

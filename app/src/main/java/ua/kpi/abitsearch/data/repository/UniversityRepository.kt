package ua.kpi.abitsearch.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ua.kpi.abitsearch.data.local.SyncMetaDao
import ua.kpi.abitsearch.data.local.SyncMetaEntity
import ua.kpi.abitsearch.data.local.UniversityDao
import ua.kpi.abitsearch.data.remote.abit.AbitApi
import ua.kpi.abitsearch.data.toDomain
import ua.kpi.abitsearch.data.toEntity
import ua.kpi.abitsearch.domain.model.Ownership
import ua.kpi.abitsearch.domain.model.University

/**
 * Offline-first репозиторій університетів.
 * UI завжди читає з Room (єдине джерело правди), а refresh() оновлює Room даними сервера.
 * Сервер віддає весь реєстр одним запитом, фільтр за регіоном і формою власності - локальний SQL.
 */
/** Порівняння рядків за українською абеткою. */
val UkrainianOrder: java.text.Collator = java.text.Collator.getInstance(java.util.Locale("uk", "UA"))

class UniversityRepository(
    private val api: AbitApi,
    private val dao: UniversityDao,
    private val meta: SyncMetaDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun observe(query: String, regionCode: String?, ownership: Ownership?): Flow<List<University>> =
        dao.observe(query.trim().lowercase(), regionCode, ownership).map { list ->
            // SQLite порівнює рядки за кодами символів, і «І», «Є» опиняються перед «А».
            // Тому остаточне сортування - за правилами української абетки (Collator).
            list.map { it.toDomain() }.sortedWith(compareBy(UkrainianOrder) { it.name })
        }

    fun observe(id: Long): Flow<University?> = dao.observeById(id).map { it?.toDomain() }

    /** Завантажує реєстр, якщо кеш застарів (TTL) або force = true. */
    suspend fun refresh(force: Boolean = false): RefreshResult {
        val now = clock()
        val last = meta.updatedAt(KEY) ?: 0L
        if (!force && now - last < TTL_MS) return RefreshResult.Skipped
        return try {
            val items = api.universities().map { it.toEntity(now) }
            // Порожня відповідь означає, що сервер ще імпортує дані: кеш не чіпаємо.
            if (items.isEmpty()) return RefreshResult.Error("Сервер ще завантажує дані з ЄДЕБО, спробуйте за хвилину", hasCache = dao.count() > 0)
            dao.replaceAll(items)
            meta.put(SyncMetaEntity(KEY, now))
            RefreshResult.Success
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            RefreshResult.Error(e.toUserMessage(), hasCache = dao.count() > 0)
        }
    }

    companion object {
        private const val KEY = "universities"
        /** Сервер оновлює дані з ЄДЕБО раз на добу, тож частіше перезавантажувати немає сенсу. */
        const val TTL_MS = 24L * 60 * 60 * 1000
    }
}

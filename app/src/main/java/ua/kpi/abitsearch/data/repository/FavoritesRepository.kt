package ua.kpi.abitsearch.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ua.kpi.abitsearch.data.local.FavoriteDao
import ua.kpi.abitsearch.data.local.FavoriteEntity
import ua.kpi.abitsearch.data.toDomain
import ua.kpi.abitsearch.domain.model.Favorite
import ua.kpi.abitsearch.domain.model.FavoriteType

/** Обране (програми й університети) зберігається лише на пристрої. */
class FavoritesRepository(
    private val dao: FavoriteDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun observeAll(): Flow<List<Favorite>> = dao.observeAll().map { l -> l.map { it.toDomain() } }

    fun isFavorite(type: FavoriteType, refId: Long): Flow<Boolean> = dao.isFavorite(type, refId)

    suspend fun toggle(type: FavoriteType, refId: Long, title: String, subtitle: String, current: Boolean) {
        dao.toggle(FavoriteEntity(type, refId, title, subtitle, clock()), current)
    }

    suspend fun remove(type: FavoriteType, refId: Long) = dao.delete(type, refId)

    suspend fun favoriteProgramIds(): List<Long> = dao.getByType(FavoriteType.PROGRAM).map { it.refId }
}

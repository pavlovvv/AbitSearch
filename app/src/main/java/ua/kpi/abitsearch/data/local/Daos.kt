package ua.kpi.abitsearch.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ua.kpi.abitsearch.domain.model.EducationLevel
import ua.kpi.abitsearch.domain.model.FavoriteType
import ua.kpi.abitsearch.domain.model.Ownership

@Dao
interface UniversityDao {
    @Query(
        """
        SELECT * FROM universities
        WHERE searchName LIKE '%' || :query || '%'
          AND (:regionCode IS NULL OR regionCode = :regionCode)
          AND (:ownership IS NULL OR ownership = :ownership)
        ORDER BY searchName
        """
    )
    fun observe(query: String, regionCode: String?, ownership: Ownership?): Flow<List<UniversityEntity>>

    @Query("SELECT * FROM universities WHERE id = :id")
    fun observeById(id: Long): Flow<UniversityEntity?>

    @Query("SELECT COUNT(*) FROM universities")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(items: List<UniversityEntity>)

    @Query("DELETE FROM universities")
    suspend fun deleteAll()

    /** Реєстр приходить повним списком: замінюємо кеш в одній транзакції (закриті заклади зникають). */
    @Transaction
    suspend fun replaceAll(items: List<UniversityEntity>) {
        deleteAll()
        upsertAll(items)
    }
}

@Dao
interface ProgramDao {
    /** minYear = рік останнього набору - лише актуальні програми; 0 - разом з архівними. */
    @Query(
        """
        SELECT * FROM programs
        WHERE universityId = :universityId AND level = :level AND lastYear >= :minYear
          AND (searchName LIKE '%' || :query || '%' OR specialtyCode LIKE '%' || :query || '%')
        ORDER BY lastYear DESC, budget + contract DESC
        """
    )
    fun observeByUniversity(universityId: Long, level: EducationLevel, minYear: Int, query: String): Flow<List<ProgramEntity>>

    /** Рейтинг закладів за кількістю зарахованих на спеціальність. */
    @Query(
        """
        SELECT * FROM programs
        WHERE specialtyKey = :key AND level = :level AND lastYear = :year
        ORDER BY budget + contract DESC
        """
    )
    fun observeBySpecialty(key: String, level: EducationLevel, year: Int): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE id = :id")
    fun observeById(id: Long): Flow<ProgramEntity?>

    @Query("SELECT * FROM program_stats WHERE programId = :programId ORDER BY year")
    fun observeStats(programId: Long): Flow<List<ProgramStatEntity>>

    @Query("SELECT MAX(lastYear) FROM programs WHERE universityId = :universityId")
    fun observeLatestYear(universityId: Long): Flow<Int?>

    @Query("SELECT COUNT(*) FROM programs WHERE universityId = :universityId")
    suspend fun countByUniversity(universityId: Long): Int

    @Query("SELECT COUNT(*) FROM programs WHERE specialtyKey = :key AND level = :level")
    suspend fun countBySpecialty(key: String, level: EducationLevel): Int

    @Query("SELECT COUNT(*) FROM program_stats WHERE programId = :programId")
    suspend fun statsCount(programId: Long): Int

    @Query("SELECT * FROM programs WHERE id = :id")
    suspend fun getById(id: Long): ProgramEntity?

    @Upsert
    suspend fun upsertAll(items: List<ProgramEntity>)

    /**
     * Список програм приходить без ліцензії (вона є лише в детальній відповіді),
     * тому при оновленні списку вже завантажені дані ліцензії не затираються.
     */
    @Transaction
    suspend fun upsertKeepingLicense(items: List<ProgramEntity>) {
        upsertAll(items.map { new ->
            val old = getById(new.id)
            if (old == null || new.licenseVolume != null || new.accredited != null) new
            else new.copy(licenseVolume = old.licenseVolume, accredited = old.accredited, accreditationUntil = old.accreditationUntil)
        })
    }

    @Query("DELETE FROM program_stats WHERE programId = :programId")
    suspend fun deleteStats(programId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(items: List<ProgramStatEntity>)

    /** Програма і вся її статистика оновлюються атомарно. */
    @Transaction
    suspend fun replaceDetail(program: ProgramEntity, stats: List<ProgramStatEntity>) {
        upsertAll(listOf(program))
        deleteStats(program.id)
        insertStats(stats)
    }
}

@Dao
interface SpecialtyDao {
    @Query(
        """
        SELECT * FROM specialties
        WHERE level = :level AND (searchName LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%')
        ORDER BY name
        """
    )
    fun observe(level: EducationLevel, query: String): Flow<List<SpecialtyEntity>>

    @Query("SELECT * FROM specialties WHERE `key` = :key AND level = :level")
    fun observeOne(key: String, level: EducationLevel): Flow<SpecialtyEntity?>

    @Query("SELECT COUNT(*) FROM specialties WHERE level = :level")
    suspend fun count(level: EducationLevel): Int

    @Query("DELETE FROM specialties WHERE level = :level")
    suspend fun deleteLevel(level: EducationLevel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SpecialtyEntity>)

    @Transaction
    suspend fun replaceLevel(level: EducationLevel, items: List<SpecialtyEntity>) {
        deleteLevel(level)
        insertAll(items)
    }
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE type = :type AND refId = :refId)")
    fun isFavorite(type: FavoriteType, refId: Long): Flow<Boolean>

    @Query("SELECT * FROM favorites WHERE type = :type")
    suspend fun getByType(type: FavoriteType): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE type = :type AND refId = :refId")
    suspend fun delete(type: FavoriteType, refId: Long)

    @Transaction
    suspend fun toggle(item: FavoriteEntity, currentlyFavorite: Boolean) {
        if (currentlyFavorite) delete(item.type, item.refId) else insert(item)
    }
}

@Dao
interface SyncMetaDao {
    @Query("SELECT updatedAt FROM sync_meta WHERE `key` = :key")
    suspend fun updatedAt(key: String): Long?

    @Upsert
    suspend fun put(meta: SyncMetaEntity)

    @Query("DELETE FROM sync_meta")
    suspend fun clear()
}

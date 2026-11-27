package ua.kpi.abitsearch.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ua.kpi.abitsearch.domain.model.EducationLevel
import ua.kpi.abitsearch.domain.model.FavoriteType
import ua.kpi.abitsearch.domain.model.Ownership

/**
 * searchName - назва в нижньому регістрі. SQLite LIKE не враховує регістр
 * лише для латиниці, тому для кирилиці пошук іде по нормалізованому полю.
 */
@Entity(
    tableName = "universities",
    indices = [Index("searchName"), Index("regionCode")],
)
data class UniversityEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val shortName: String,
    val searchName: String,
    val regionCode: String,
    val regionName: String,
    val ownership: Ownership,
    val site: String?,
    val address: String?,
    val programsCount: Int,
    val updatedAt: Long,
)

/**
 * Освітня програма. FK на universities немає навмисно: список програм спеціальності
 * приходить з різних закладів, і назва закладу зберігається тут же (денормалізація для офлайну).
 * Поля license* заповнюються, коли відкрито детальний екран програми.
 */
@Entity(
    tableName = "programs",
    indices = [Index("universityId"), Index(value = ["specialtyKey", "level"])],
)
data class ProgramEntity(
    @PrimaryKey val id: Long,
    val universityId: Long,
    val universityName: String,
    val level: EducationLevel,
    val base: String,
    val specialtyCode: String,
    val specialtyName: String,
    val specialtyKey: String,
    val name: String,
    val searchName: String,
    val lastYear: Int,
    val budget: Int,
    val contract: Int,
    val licenseVolume: Int?,
    val accredited: Boolean?,
    val accreditationUntil: String?,
    val updatedAt: Long,
)

/** Зарахування за рік. Видалення програми каскадно видаляє її статистику. */
@Entity(
    tableName = "program_stats",
    primaryKeys = ["programId", "year"],
    foreignKeys = [ForeignKey(
        entity = ProgramEntity::class,
        parentColumns = ["id"],
        childColumns = ["programId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class ProgramStatEntity(
    val programId: Long,
    val year: Int,
    val dayBudget: Int,
    val dayContract: Int,
    val partBudget: Int,
    val partContract: Int,
    val eveningBudget: Int,
    val eveningContract: Int,
)

/** Спеціальності останнього набору; ключ - нормалізована назва (коди змінилися у 2025 році). */
@Entity(tableName = "specialties", primaryKeys = ["key", "level"], indices = [Index("searchName")])
data class SpecialtyEntity(
    val key: String,
    val level: EducationLevel,
    val code: String,
    val name: String,
    val searchName: String,
    val year: Int,
    val programsCount: Int,
    val universitiesCount: Int,
    val budget: Int,
    val contract: Int,
)

/** Обране зберігається лише на пристрої; title/subtitle дозволяють показати список офлайн. */
@Entity(tableName = "favorites", primaryKeys = ["type", "refId"])
data class FavoriteEntity(
    val type: FavoriteType,
    val refId: Long,
    val title: String,
    val subtitle: String,
    val addedAt: Long,
)

/** Час останнього успішного оновлення ресурсу - для стратегії кешування (TTL). */
@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val key: String,
    val updatedAt: Long,
)

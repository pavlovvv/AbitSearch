package ua.kpi.abitsearch.data.local

import androidx.room.TypeConverter
import ua.kpi.abitsearch.domain.model.EducationLevel
import ua.kpi.abitsearch.domain.model.FavoriteType
import ua.kpi.abitsearch.domain.model.Ownership

/** Enum-и зберігаються в БД як рядки (name), щоб не залежати від порядку констант. */
class Converters {
    @TypeConverter fun ownershipToString(v: Ownership) = v.name
    @TypeConverter fun stringToOwnership(v: String) = Ownership.valueOf(v)
    @TypeConverter fun levelToString(v: EducationLevel) = v.name
    @TypeConverter fun stringToLevel(v: String) = EducationLevel.valueOf(v)
    @TypeConverter fun favTypeToString(v: FavoriteType) = v.name
    @TypeConverter fun stringToFavType(v: String) = FavoriteType.valueOf(v)
}

package ua.kpi.abitsearch.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UniversityEntity::class,
        ProgramEntity::class,
        ProgramStatEntity::class,
        SpecialtyEntity::class,
        FavoriteEntity::class,
        SyncMetaEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun universityDao(): UniversityDao
    abstract fun programDao(): ProgramDao
    abstract fun specialtyDao(): SpecialtyDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun syncMetaDao(): SyncMetaDao

    companion object {
        /**
         * Версія 2: демонстраційні таблиці offers/applicants/applications замінено
         * реальними даними (programs, program_stats, specialties). Локальна БД - лише кеш
         * даних сервера, тож при зміні схеми її безпечно створити заново; обране теж
         * перебудовується, бо старі ідентифікатори пропозицій більше не існують.
         */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "abitsearch.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}

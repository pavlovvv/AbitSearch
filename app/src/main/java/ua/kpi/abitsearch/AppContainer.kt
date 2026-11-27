package ua.kpi.abitsearch

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ua.kpi.abitsearch.data.auth.AuthDataStore
import ua.kpi.abitsearch.data.auth.AuthInterceptor
import ua.kpi.abitsearch.data.local.AppDatabase
import ua.kpi.abitsearch.data.remote.abit.AbitApi
import ua.kpi.abitsearch.data.repository.AuthRepository
import ua.kpi.abitsearch.data.repository.FavoritesRepository
import ua.kpi.abitsearch.data.repository.ProgramRepository
import ua.kpi.abitsearch.data.repository.SettingsRepository
import ua.kpi.abitsearch.data.repository.UniversityRepository
import java.util.concurrent.TimeUnit

/** Ручне впровадження залежностей: усі одиночні об'єкти створюються тут один раз. */
class AppContainer(context: Context) {

    /** Область корутин застосунку - для дій, що не належать жодному екрану. */
    private val appScope = CoroutineScope(SupervisorJob())

    /** Gson для розбору JSON (як у ПЗ 10). lenient - щоб не падати на дрібних відхиленнях формату. */
    private val gson: Gson = GsonBuilder().setLenient().create()

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    val authDataStore = AuthDataStore(context)

    /** HTTP-клієнт сервера AbitSearch: JWT у заголовку Authorization, на 401 сесія завершується. */
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor({ authDataStore.cachedToken }) { appScope.launch { authRepository.expireSession() } })
        .addInterceptor(logging)
        .build()

    private val api: AbitApi = Retrofit.Builder()
        .baseUrl(BuildConfig.ABIT_API_URL)
        .client(http)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(AbitApi::class.java)

    val database: AppDatabase = AppDatabase.create(context)

    val authRepository = AuthRepository(api, authDataStore)
    val universityRepository = UniversityRepository(api, database.universityDao(), database.syncMetaDao())
    val programRepository = ProgramRepository(api, database.programDao(), database.specialtyDao(), database.syncMetaDao())
    val favoritesRepository = FavoritesRepository(database.favoriteDao())
    val settingsRepository = SettingsRepository(context)
}

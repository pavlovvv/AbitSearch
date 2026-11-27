package ua.kpi.abitsearch.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ua.kpi.abitsearch.AbitSearchApp
import ua.kpi.abitsearch.data.repository.RefreshResult
import java.util.concurrent.TimeUnit

/**
 * Фонове оновлення: раз на 6 годин за наявності мережі оновлює реєстр університетів
 * і програми з обраного, щоб користувач бачив свіжі дані навіть офлайн.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as AbitSearchApp).container
        // Без входу сервер відповідає 401 - оновлювати нічого.
        if (container.authDataStore.cachedToken.isNullOrBlank()) return Result.success()
        val universities = container.universityRepository.refresh()
        container.favoritesRepository.favoriteProgramIds().forEach { container.programRepository.refreshProgram(it) }
        container.settingsRepository.setLastSync(System.currentTimeMillis())
        return if (universities is RefreshResult.Error && runAttemptCount < 3) Result.retry() else Result.success()
    }

    companion object {
        private const val NAME = "abitsearch-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

package ua.kpi.abitsearch

import android.app.Application
import ua.kpi.abitsearch.data.sync.SyncWorker

class AbitSearchApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncWorker.schedule(this)
    }
}

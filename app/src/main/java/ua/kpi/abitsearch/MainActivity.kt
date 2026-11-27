package ua.kpi.abitsearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.kpi.abitsearch.domain.model.ThemeMode
import ua.kpi.abitsearch.ui.navigation.AppRoot
import ua.kpi.abitsearch.ui.theme.AbitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as AbitSearchApp).container.settingsRepository
        setContent {
            val mode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.DARK)
            AbitTheme(mode) { AppRoot() }
        }
    }
}

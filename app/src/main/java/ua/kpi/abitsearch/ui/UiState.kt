package ua.kpi.abitsearch.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ua.kpi.abitsearch.AbitSearchApp
import ua.kpi.abitsearch.AppContainer

/** Стан екрана - відповідає класу UiState з діаграми класів (розділ 1, рис. 1.5). */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T, val offlineMessage: String? = null) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
}

/**
 * Зводить дані з Room і стан оновлення в один UiState:
 * дані є → Success (з банером, якщо оновлення не вдалося); даних немає → Loading / Error / Empty.
 */
fun <T> listUiState(items: List<T>, refreshing: Boolean, error: String?): UiState<List<T>> = when {
    items.isNotEmpty() -> UiState.Success(items, offlineMessage = error)
    refreshing -> UiState.Loading
    error != null -> UiState.Error(error)
    else -> UiState.Empty
}

/** Створює ViewModel з доступом до AppContainer (ручний DI замість Hilt). */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val app = LocalContext.current.applicationContext as AbitSearchApp
    return viewModel(key = key, factory = viewModelFactory { initializer { create(app.container) } })
}

package ua.kpi.abitsearch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.kpi.abitsearch.BuildConfig
import ua.kpi.abitsearch.data.local.AppDatabase
import ua.kpi.abitsearch.data.repository.FavoritesRepository
import ua.kpi.abitsearch.data.repository.AuthRepository
import ua.kpi.abitsearch.domain.model.AuthSession
import ua.kpi.abitsearch.domain.model.Role
import ua.kpi.abitsearch.ui.auth.RoleBadge
import ua.kpi.abitsearch.ui.components.Avatar
import ua.kpi.abitsearch.ui.components.SecondaryButton
import ua.kpi.abitsearch.ui.components.plural
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Dns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.kpi.abitsearch.data.repository.SettingsRepository
import ua.kpi.abitsearch.domain.model.Favorite
import ua.kpi.abitsearch.domain.model.FavoriteType
import ua.kpi.abitsearch.domain.model.ThemeMode
import ua.kpi.abitsearch.ui.appViewModel
import ua.kpi.abitsearch.ui.components.abbreviation
import ua.kpi.abitsearch.ui.components.GlassCard
import ua.kpi.abitsearch.ui.components.LargeTitle
import ua.kpi.abitsearch.ui.components.MessageState
import ua.kpi.abitsearch.ui.components.PrimaryButton
import ua.kpi.abitsearch.ui.components.SectionLabel
import ua.kpi.abitsearch.ui.components.Segmented
import ua.kpi.abitsearch.ui.components.SubtitleText
import ua.kpi.abitsearch.ui.components.TitleText
import ua.kpi.abitsearch.ui.theme.Abit

class FavoritesViewModel(private val repo: FavoritesRepository) : ViewModel() {
    val items: StateFlow<List<Favorite>?> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun remove(f: Favorite) {
        viewModelScope.launch { repo.remove(f.type, f.refId) }
    }
}

@Composable
fun FavoritesScreen(
    onOpenProgram: (Long) -> Unit,
    onOpenUniversity: (Long) -> Unit,
    onFindUniversity: () -> Unit,
    contentPadding: PaddingValues,
) {
    val vm = appViewModel { FavoritesViewModel(it.favoritesRepository) }
    val all by vm.items.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(FavoriteType.PROGRAM) }
    val list = all.orEmpty().filter { it.type == tab }
    val programsCount = all.orEmpty().count { it.type == FavoriteType.PROGRAM }
    val universitiesCount = all.orEmpty().size - programsCount

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = contentPadding.calculateTopPadding() + 40.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            LargeTitle("Обране")
            Segmented(
                options = FavoriteType.entries,
                selected = tab,
                label = { if (it == FavoriteType.PROGRAM) "Програми · $programsCount" else "Університети · $universitiesCount" },
                onSelect = { tab = it },
            )
        }
        if (all != null && list.isEmpty()) {
            item {
                MessageState(
                    Icons.Outlined.StarOutline,
                    "Тут поки порожньо",
                    "Натисніть ☆ на сторінці освітньої програми або університету, щоб зберегти їх тут",
                    actionText = "Знайти університет",
                    onAction = onFindUniversity,
                )
            }
        } else {
            item { SectionLabel("Збережено на пристрої") }
            items(list, key = { "${it.type}-${it.refId}" }) { f ->
                FavoriteCard(f, onRemove = { vm.remove(f) }) {
                    if (f.type == FavoriteType.PROGRAM) onOpenProgram(f.refId) else onOpenUniversity(f.refId)
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(f: Favorite, onRemove: () -> Unit, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (f.type == FavoriteType.UNIVERSITY) {
                Avatar(abbreviation(f.title))
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                TitleText(f.title)
                SubtitleText(f.subtitle)
            }
            Icon(
                Icons.Filled.Star, "Прибрати з обраного", tint = Color(0xFFFBBF24),
                modifier = Modifier
                    .size(22.dp)
                    .clickable(onClick = onRemove),
            )
        }
    }
}

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val db: AppDatabase,
    private val auth: AuthRepository,
) : ViewModel() {
    val theme: StateFlow<ThemeMode> = settings.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.DARK)
    val lastSync: StateFlow<Long?> = settings.lastSync.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val session: StateFlow<AuthSession> = auth.session.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthSession.Loading)

    /** Профіль із захищеного ресурсу /auth/me - підтверджує, що сервер приймає токен. */
    private val _profile = MutableStateFlow<String?>(null)
    val profileStatus: StateFlow<String?> = _profile.asStateFlow()

    /** Статистика з ресурсу лише для адміністратора /admin/stats. */
    private val _admin = MutableStateFlow<String?>(null)
    val adminStats: StateFlow<String?> = _admin.asStateFlow()

    init {
        viewModelScope.launch {
            _profile.value = auth.profile().fold({ "підтверджено сервером" }, { it.message })
        }
    }

    fun loadAdminStats() {
        viewModelScope.launch {
            _admin.value = auth.adminStats().fold(
                { st ->
                    val users = plural(st.usersCount, "користувач", "користувачі", "користувачів") + ", з них адміністраторів: ${st.adminsCount}"
                    val import = when {
                        st.importRunning -> "Імпорт ЄДЕБО виконується зараз"
                        st.lastImport == null -> "Імпорт ЄДЕБО ще не виконувався"
                        st.lastImport.status == "SUCCESS" -> "Останній імпорт: " + formatDate(st.lastImport.finishedAt ?: st.lastImport.startedAt) +
                            ", " + plural(st.lastImport.programs, "програма", "програми", "програм") + ", ${st.lastImport.stats} записів статистики"
                        else -> "Останній імпорт не вдався: ${st.lastImport.message ?: st.lastImport.status}"
                    }
                    "$users\n$import"
                },
                { it.message },
            )
        }
    }

    private val _import = MutableStateFlow<String?>(null)
    val importStatus: StateFlow<String?> = _import.asStateFlow()

    /** Ручний запуск імпорту на сервері; сам імпорт іде у фоні на сервері. */
    fun startImport() {
        viewModelScope.launch { _import.value = auth.startImport().fold({ it }, { it.message }) }
    }

    fun setTheme(mode: ThemeMode) { viewModelScope.launch { settings.setThemeMode(mode) } }

    /** Скидає час останнього оновлення - наступне відкриття екранів завантажить дані заново. */
    fun clearCache() { viewModelScope.launch { db.syncMetaDao().clear() } }

    fun logout() { viewModelScope.launch { auth.logout() } }
}

@Composable
fun SettingsScreen(contentPadding: PaddingValues) {
    val vm = appViewModel { SettingsViewModel(it.settingsRepository, it.database, it.authRepository) }
    val session by vm.session.collectAsStateWithLifecycle()
    val profileStatus by vm.profileStatus.collectAsStateWithLifecycle()
    val adminStats by vm.adminStats.collectAsStateWithLifecycle()
    val importStatus by vm.importStatus.collectAsStateWithLifecycle()
    val user = (session as? AuthSession.LoggedIn)?.user
    val expiresAt = (session as? AuthSession.LoggedIn)?.expiresAt
    val theme by vm.theme.collectAsStateWithLifecycle()
    val lastSync by vm.lastSync.collectAsStateWithLifecycle()
    var cacheCleared by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = contentPadding.calculateTopPadding() + 40.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
    ) {
        item {
            LargeTitle("Налаштування")
            if (user != null) {
                SectionLabel("Акаунт")
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(user.name.split(" ").take(2).joinToString("") { it.take(1) }, circle = true)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            TitleText(user.name, maxLines = 1)
                            SubtitleText(user.email, maxLines = 1)
                        }
                        RoleBadge(user.role)
                    }
                    Spacer(Modifier.height(10.dp))
                    SubtitleText("Профіль: ${profileStatus ?: "перевірка…"}")
                    expiresAt?.let { SubtitleText("Токен дійсний до " + formatDate(it)) }
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton("Вийти з акаунта", vm::logout, Modifier.fillMaxWidth())
                }
            }
            SectionLabel("Оформлення")
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingIcon(Icons.Outlined.DarkMode, Color(0xFF6E5CF6))
                    Spacer(Modifier.width(12.dp))
                    Text("Тема", color = Abit.colors.text, fontSize = 15.sp)
                }
                Spacer(Modifier.height(12.dp))
                Segmented(ThemeMode.entries, theme, { it.title }, vm::setTheme)
            }
            SectionLabel("Дані")
            GlassCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                SettingRow(
                    Icons.Outlined.Refresh, Color(0xFF3B82F6), "Фонове оновлення",
                    lastSync?.let { formatDate(it) } ?: "ще не було",
                )
                HorizontalDivider(color = Abit.colors.outline)
                SettingRow(
                    Icons.Outlined.Delete, Color(0xFFEF4444), "Оновити кеш",
                    if (cacheCleared) "готово" else "",
                ) { vm.clearCache(); cacheCleared = true }
            }
            // Розмежування доступу за роллю з JWT: розділ бачить лише адміністратор, сервер теж перевіряє роль.
            if (user?.role == Role.ADMIN) {
                SectionLabel("Адміністрування сервера")
                GlassCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                    SettingRow(Icons.Outlined.Group, Color(0xFFF59E0B), "Статистика сервера", if (adminStats == null) "показати" else "") { vm.loadAdminStats() }
                    adminStats?.let { SubtitleText(it, Modifier.padding(start = 56.dp, end = 14.dp, bottom = 12.dp), maxLines = 4) }
                    HorizontalDivider(color = Abit.colors.outline)
                    SettingRow(Icons.Outlined.CloudDownload, Color(0xFF10B981), "Імпорт з ЄДЕБО", if (importStatus == null) "запустити" else "") { vm.startImport() }
                    importStatus?.let { SubtitleText(it, Modifier.padding(start = 56.dp, end = 14.dp, bottom = 12.dp)) }
                }
            }
            SectionLabel("Про додаток")
            GlassCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                SettingRow(Icons.Outlined.Storage, Color(0xFF10B981), "Джерело даних", "ЄДЕБО")
                HorizontalDivider(color = Abit.colors.outline)
                SettingRow(Icons.Outlined.Dns, Color(0xFF3B82F6), "Сервер", BuildConfig.ABIT_API_URL.removePrefix("http://").removePrefix("https://").trimEnd('/'))
                HorizontalDivider(color = Abit.colors.outline)
                SettingRow(Icons.Outlined.Info, Color(0xFF8B5CF6), "Версія", BuildConfig.VERSION_NAME)
            }
            SubtitleText(
                "Реєстр закладів, зарахування за 2016-2025 роки та ліцензії - відкриті дані ЄДЕБО. Рейтингові списки з персональними даними вступників у відкритих даних не публікуються.",
                Modifier.padding(top = 14.dp, start = 4.dp, end = 4.dp), maxLines = 3,
            )
        }
    }
}

private fun formatDate(time: Long): String =
    java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale("uk")).format(java.util.Date(time))

@Composable
private fun SettingIcon(icon: ImageVector, color: Color) {
    Box(
        Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp)) }
}

@Composable
private fun SettingRow(icon: ImageVector, color: Color, title: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon, color)
        Spacer(Modifier.width(12.dp))
        Text(title, color = Abit.colors.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = Abit.colors.muted, fontSize = 14.sp)
    }
}

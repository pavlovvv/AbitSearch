package ua.kpi.abitsearch.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.kpi.abitsearch.data.repository.FavoritesRepository
import ua.kpi.abitsearch.data.repository.ProgramRepository
import ua.kpi.abitsearch.data.repository.RefreshResult
import ua.kpi.abitsearch.data.repository.UniversityRepository
import ua.kpi.abitsearch.domain.model.EducationLevel
import ua.kpi.abitsearch.domain.model.FavoriteType
import ua.kpi.abitsearch.domain.model.Program
import ua.kpi.abitsearch.domain.model.University
import ua.kpi.abitsearch.ui.UiState
import ua.kpi.abitsearch.ui.appViewModel
import ua.kpi.abitsearch.ui.components.AbitChip
import ua.kpi.abitsearch.ui.components.Avatar
import ua.kpi.abitsearch.ui.components.ChipRow
import ua.kpi.abitsearch.ui.components.CircleIconButton
import ua.kpi.abitsearch.ui.components.DetailTopBar
import ua.kpi.abitsearch.ui.components.ErrorState
import ua.kpi.abitsearch.ui.components.GlassCard
import ua.kpi.abitsearch.ui.components.MessageState
import ua.kpi.abitsearch.ui.components.OfflineBanner
import ua.kpi.abitsearch.ui.components.SearchField
import ua.kpi.abitsearch.ui.components.SectionLabel
import ua.kpi.abitsearch.ui.components.Segmented
import ua.kpi.abitsearch.ui.components.SkeletonList
import ua.kpi.abitsearch.ui.components.SubtitleText
import ua.kpi.abitsearch.ui.components.TitleText
import ua.kpi.abitsearch.ui.components.abbreviation
import ua.kpi.abitsearch.ui.listUiState
import ua.kpi.abitsearch.ui.theme.Abit

/** Поточні умови відбору програм; minYear = 0 - разом з архівними. */
private data class Filter(val level: EducationLevel, val query: String, val minYear: Int)

@OptIn(ExperimentalCoroutinesApi::class)
class UniversityViewModel(
    private val universityId: Long,
    universities: UniversityRepository,
    private val programs: ProgramRepository,
    private val favorites: FavoritesRepository,
) : ViewModel() {

    private val _level = MutableStateFlow(EducationLevel.BACHELOR)
    private val _query = MutableStateFlow("")
    private val _onlyCurrent = MutableStateFlow(true)
    private val refreshing = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    val level: StateFlow<EducationLevel> = _level.asStateFlow()
    val query: StateFlow<String> = _query.asStateFlow()
    val onlyCurrent: StateFlow<Boolean> = _onlyCurrent.asStateFlow()

    val university: StateFlow<University?> = universities.observe(universityId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Рік останнього набору в цьому закладі - для фільтра «лише актуальні програми». */
    val latestYear: StateFlow<Int?> = programs.observeLatestYear(universityId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val items: StateFlow<UiState<List<Program>>> =
        combine(
            combine(_level, _query, _onlyCurrent, latestYear) { l, q, cur, year -> Filter(l, q, if (cur) year ?: 0 else 0) }
                .flatMapLatest { f -> programs.observeByUniversity(universityId, f.level, f.minYear, f.query) },
            refreshing,
            error,
        ) { list, busy, err -> listUiState(list, busy, err) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val isFavorite: StateFlow<Boolean> = favorites.isFavorite(FavoriteType.UNIVERSITY, universityId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init { refresh(force = false) }

    fun setLevel(value: EducationLevel) { _level.value = value }
    fun setQuery(value: String) { _query.value = value }
    fun setOnlyCurrent(value: Boolean) { _onlyCurrent.value = value }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            refreshing.value = true
            error.value = (programs.refreshUniversity(universityId, force) as? RefreshResult.Error)?.message
            refreshing.value = false
        }
    }

    fun toggleFavorite() {
        val u = university.value ?: return
        viewModelScope.launch {
            favorites.toggle(FavoriteType.UNIVERSITY, u.id, u.shortName, "${u.region} · ${u.ownership.title}", isFavorite.value)
        }
    }
}

@Composable
fun UniversityScreen(universityId: Long, onBack: () -> Unit, onOpenProgram: (Long) -> Unit, contentPadding: PaddingValues) {
    val vm = appViewModel(key = "university-$universityId") {
        UniversityViewModel(universityId, it.universityRepository, it.programRepository, it.favoritesRepository)
    }
    val university by vm.university.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val level by vm.level.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val onlyCurrent by vm.onlyCurrent.collectAsStateWithLifecycle()
    val latestYear by vm.latestYear.collectAsStateWithLifecycle()
    val fav by vm.isFavorite.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            DetailTopBar("Університет", onBack) {
                CircleIconButton(
                    if (fav) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    if (fav) "Прибрати з обраного" else "В обране",
                    tint = if (fav) Color(0xFFFBBF24) else Abit.colors.text,
                    onClick = vm::toggleFavorite,
                )
            }
            university?.let { UniversityHeader(it, items, latestYear) }
            Spacer(Modifier.height(10.dp))
            Segmented(EducationLevel.entries, level, { it.title }, vm::setLevel)
            Spacer(Modifier.height(10.dp))
            SearchField(query, vm::setQuery, "Програма, спеціальність або код")
            ChipRow {
                AbitChip(latestYear?.let { "Набір $it" } ?: "Актуальні", onlyCurrent) { vm.setOnlyCurrent(true) }
                AbitChip("Усі роки", !onlyCurrent) { vm.setOnlyCurrent(false) }
            }
        }
        when (val s = items) {
            UiState.Loading -> item { SkeletonList(4) }
            UiState.Empty -> item { MessageState(Icons.Outlined.Inbox, "Програм немає", "Для цього рівня або запиту в ЄДЕБО немає даних про зарахування") }
            is UiState.Error -> item { ErrorState(s.message) { vm.refresh() } }
            is UiState.Success -> {
                item {
                    s.offlineMessage?.let { OfflineBanner(it) }
                    SectionLabel("Освітні програми · ${s.data.size}")
                }
                items(s.data, key = { it.id }) { p -> ProgramCard(p, showUniversity = false, latestYear = latestYear) { onOpenProgram(p.id) } }
            }
        }
    }
}

@Composable
private fun UniversityHeader(u: University, items: UiState<List<Program>>, latestYear: Int?) {
    val c = Abit.colors
    val current = (items as? UiState.Success)?.data.orEmpty().filter { it.lastYear == latestYear }
    GlassCard(Modifier.fillMaxWidth().padding(top = 6.dp), padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(abbreviation(u.shortName), size = 54.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                TitleText(u.name)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = c.muted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    SubtitleText("${u.region} · ${u.ownership.title}", maxLines = 1)
                }
                u.site?.let { SubtitleText(it, maxLines = 1) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat(current.size.toString(), "програм", Modifier.weight(1f))
            MiniStat(current.sumOf { it.budget }.toString(), "бюджет", Modifier.weight(1f))
            MiniStat(current.sumOf { it.contract }.toString(), "контракт", Modifier.weight(1f))
        }
        latestYear?.let {
            SubtitleText("Зараховано у $it році, обраний рівень освіти", Modifier.padding(top = 8.dp), maxLines = 1)
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier) {
    GlassCard(modifier, radius = 14.dp, padding = 10.dp) {
        Text(value, color = Abit.colors.text, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(label, color = Abit.colors.muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

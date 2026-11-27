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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.kpi.abitsearch.data.repository.ProgramRepository
import ua.kpi.abitsearch.data.repository.RefreshResult
import ua.kpi.abitsearch.domain.model.EducationLevel
import ua.kpi.abitsearch.domain.model.Program
import ua.kpi.abitsearch.domain.model.Specialty
import ua.kpi.abitsearch.ui.UiState
import ua.kpi.abitsearch.ui.appViewModel
import ua.kpi.abitsearch.ui.components.CodeTag
import ua.kpi.abitsearch.ui.components.DetailTopBar
import ua.kpi.abitsearch.ui.components.ErrorState
import ua.kpi.abitsearch.ui.components.GlassCard
import ua.kpi.abitsearch.ui.components.LargeTitle
import ua.kpi.abitsearch.ui.components.MessageState
import ua.kpi.abitsearch.ui.components.OfflineBanner
import ua.kpi.abitsearch.ui.components.SearchField
import ua.kpi.abitsearch.ui.components.SectionLabel
import ua.kpi.abitsearch.ui.components.Segmented
import ua.kpi.abitsearch.ui.components.SkeletonList
import ua.kpi.abitsearch.ui.components.SubtitleText
import ua.kpi.abitsearch.ui.components.TitleText
import ua.kpi.abitsearch.ui.components.plural
import ua.kpi.abitsearch.ui.listUiState
import ua.kpi.abitsearch.ui.theme.Abit
import ua.kpi.abitsearch.ui.theme.budgetColor

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SpecialtiesViewModel(private val programs: ProgramRepository) : ViewModel() {
    private val _level = MutableStateFlow(EducationLevel.BACHELOR)
    private val _query = MutableStateFlow("")
    private val refreshing = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    val level: StateFlow<EducationLevel> = _level.asStateFlow()
    val query: StateFlow<String> = _query.asStateFlow()

    val items: StateFlow<UiState<List<Specialty>>> =
        combine(
            combine(_level, _query.debounce(200)) { l, q -> l to q }.flatMapLatest { (l, q) -> programs.observeSpecialties(l, q) },
            refreshing,
            error,
        ) { list, busy, err -> listUiState(list, busy, err) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init { refresh(force = false) }

    fun setLevel(value: EducationLevel) {
        _level.value = value
        refresh(force = false)
    }

    fun setQuery(value: String) { _query.value = value }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            refreshing.value = true
            error.value = (programs.refreshSpecialties(_level.value, force) as? RefreshResult.Error)?.message
            refreshing.value = false
        }
    }
}

/**
 * Вкладка «Спеціальності»: пошук спеціальності й порівняння закладів за кількістю зарахованих.
 * Замінює пошук абітурієнтів із розділів 1-3: персональні рейтингові списки не є відкритими даними.
 */
@Composable
fun SpecialtiesScreen(onOpen: (Specialty) -> Unit, contentPadding: PaddingValues) {
    val vm = appViewModel { SpecialtiesViewModel(it.programRepository) }
    val items by vm.items.collectAsStateWithLifecycle()
    val level by vm.level.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = contentPadding.calculateTopPadding() + 40.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            LargeTitle("Спеціальності")
            Segmented(EducationLevel.entries, level, { it.title }, vm::setLevel)
            Spacer(Modifier.height(10.dp))
            SearchField(query, vm::setQuery, "Назва або код спеціальності")
        }
        when (val s = items) {
            UiState.Loading -> item { SkeletonList(5) }
            UiState.Empty -> item { MessageState(Icons.Outlined.SearchOff, "Нічого не знайдено", "Спробуйте іншу назву або код") }
            is UiState.Error -> item { ErrorState(s.message) { vm.refresh() } }
            is UiState.Success -> {
                item {
                    s.offlineMessage?.let { OfflineBanner(it) }
                    SectionLabel("Набір ${s.data.first().year} · ${s.data.size}")
                }
                items(s.data, key = { it.key }) { sp -> SpecialtyCard(sp) { onOpen(sp) } }
            }
        }
    }
}

@Composable
private fun SpecialtyCard(s: Specialty, onClick: () -> Unit) {
    val c = Abit.colors
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CodeTag(s.code)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                TitleText(s.name)
                SubtitleText(plural(s.universitiesCount, "заклад", "заклади", "закладів") + " · " + plural(s.programsCount, "програма", "програми", "програм"), maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(s.total.toString(), color = c.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("бюдж. ${s.budget}", color = budgetColor(c.isDark), fontSize = 11.sp)
            }
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.faint)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SpecialtyViewModel(
    private val key: String,
    private val level: EducationLevel,
    private val programs: ProgramRepository,
) : ViewModel() {
    private val refreshing = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    val specialty: StateFlow<Specialty?> = programs.observeSpecialty(key, level)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val items: StateFlow<UiState<List<Program>>> =
        combine(
            specialty.flatMapLatest { s -> if (s == null) flowOf(emptyList()) else programs.observeBySpecialty(key, level, s.year) },
            refreshing,
            error,
        ) { list, busy, err -> listUiState(list, busy, err) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init { refresh(force = false) }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            refreshing.value = true
            error.value = (programs.refreshSpecialtyPrograms(key, level, force) as? RefreshResult.Error)?.message
            refreshing.value = false
        }
    }
}

@Composable
fun SpecialtyScreen(key: String, level: EducationLevel, onBack: () -> Unit, onOpenProgram: (Long) -> Unit, contentPadding: PaddingValues) {
    val vm = appViewModel(key = "specialty-$level-$key") { SpecialtyViewModel(key, level, it.programRepository) }
    val specialty by vm.specialty.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val c = Abit.colors

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            DetailTopBar("Спеціальність", onBack)
            specialty?.let { s ->
                Column(Modifier.padding(top = 6.dp, bottom = 4.dp)) {
                    CodeTag(s.code)
                    Spacer(Modifier.height(6.dp))
                    Text(s.name, color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                    SubtitleText("${s.level.title} · набір ${s.year}: зараховано ${s.total}, з них на бюджет ${s.budget}", maxLines = 3)
                }
            }
            SectionLabel("Заклади за кількістю зарахованих")
        }
        when (val s = items) {
            UiState.Loading -> item { SkeletonList(5) }
            UiState.Empty -> item { MessageState(Icons.Outlined.SearchOff, "Програм немає", "За цією спеціальністю немає даних останнього набору") }
            is UiState.Error -> item { ErrorState(s.message) { vm.refresh() } }
            is UiState.Success -> {
                s.offlineMessage?.let { msg -> item { OfflineBanner(msg) } }
                itemsIndexed(s.data, key = { _, p -> p.id }) { i, p ->
                    ProgramCard(p, showUniversity = true, rank = i + 1) { onOpenProgram(p.id) }
                }
            }
        }
    }
}

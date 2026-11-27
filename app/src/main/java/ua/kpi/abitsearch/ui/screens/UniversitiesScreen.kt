package ua.kpi.abitsearch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.kpi.abitsearch.data.Regions
import ua.kpi.abitsearch.data.repository.RefreshResult
import ua.kpi.abitsearch.data.repository.UniversityRepository
import ua.kpi.abitsearch.domain.model.Ownership
import ua.kpi.abitsearch.domain.model.University
import ua.kpi.abitsearch.ui.UiState
import ua.kpi.abitsearch.ui.appViewModel
import ua.kpi.abitsearch.ui.components.AbitChip
import ua.kpi.abitsearch.ui.components.Avatar
import ua.kpi.abitsearch.ui.components.ChipRow
import ua.kpi.abitsearch.ui.components.CircleIconButton
import ua.kpi.abitsearch.ui.components.ErrorState
import ua.kpi.abitsearch.ui.components.GlassCard
import ua.kpi.abitsearch.ui.components.LargeTitle
import ua.kpi.abitsearch.ui.components.MessageState
import ua.kpi.abitsearch.ui.components.OfflineBanner
import ua.kpi.abitsearch.ui.components.PrimaryButton
import ua.kpi.abitsearch.ui.components.SearchField
import ua.kpi.abitsearch.ui.components.SecondaryButton
import ua.kpi.abitsearch.ui.components.SectionLabel
import ua.kpi.abitsearch.ui.components.Segmented
import ua.kpi.abitsearch.ui.components.SkeletonList
import ua.kpi.abitsearch.ui.components.SubtitleText
import ua.kpi.abitsearch.ui.components.TitleText
import ua.kpi.abitsearch.ui.components.abbreviation
import ua.kpi.abitsearch.ui.components.plural
import ua.kpi.abitsearch.ui.listUiState
import ua.kpi.abitsearch.ui.theme.Abit

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class UniversitiesViewModel(private val repo: UniversityRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val _region = MutableStateFlow<String?>(null)
    private val _ownership = MutableStateFlow<Ownership?>(null)
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val queryState: StateFlow<String> = query.asStateFlow()
    val region: StateFlow<String?> = _region.asStateFlow()
    val ownership: StateFlow<Ownership?> = _ownership.asStateFlow()
    val isRefreshing: StateFlow<Boolean> = refreshing.asStateFlow()

    private val items = combine(query.debounce(250), _region, _ownership) { q, r, o -> Triple(q, r, o) }
        .flatMapLatest { (q, r, o) -> repo.observe(q, r, o) }

    val uiState: StateFlow<UiState<List<University>>> =
        combine(items, refreshing, error) { list, busy, err -> listUiState(list, busy, err) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init {
        refresh(force = false)
    }

    fun onQuery(value: String) { query.value = value }

    fun applyFilter(regionCode: String?, ownership: Ownership?) {
        _region.value = regionCode
        _ownership.value = ownership
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            refreshing.value = true
            val result = repo.refresh(force)
            error.value = (result as? RefreshResult.Error)?.message
            refreshing.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversitiesScreen(onOpen: (Long) -> Unit, contentPadding: PaddingValues) {
    val vm = appViewModel { UniversitiesViewModel(it.universityRepository) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val query by vm.queryState.collectAsStateWithLifecycle()
    val region by vm.region.collectAsStateWithLifecycle()
    val ownership by vm.ownership.collectAsStateWithLifecycle()
    val refreshing by vm.isRefreshing.collectAsStateWithLifecycle()
    var showFilter by remember { mutableStateOf(false) }

    PullToRefreshBox(isRefreshing = refreshing, onRefresh = { vm.refresh() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    CircleIconButton(Icons.Filled.Tune, "Фільтр") { showFilter = true }
                }
                LargeTitle("Університети")
                SearchField(query, vm::onQuery, "Пошук за назвою")
                ChipRow {
                    AbitChip("Усі регіони", region == null) { vm.applyFilter(null, ownership) }
                    Regions.all.take(5).forEach { r ->
                        AbitChip(r.name, region == r.code) { vm.applyFilter(r.code, ownership) }
                    }
                }
            }
            when (val s = state) {
                UiState.Loading -> item { SkeletonList() }
                UiState.Empty -> item {
                    MessageState(Icons.Outlined.SearchOff, "Нічого не знайдено", "Спробуйте змінити запит або фільтр")
                }
                is UiState.Error -> item { ErrorState(s.message) { vm.refresh() } }
                is UiState.Success -> {
                    item {
                        s.offlineMessage?.let { OfflineBanner(it) }
                        SectionLabel("Знайдено ${s.data.size}")
                    }
                    items(s.data, key = { it.id }) { u -> UniversityCard(u) { onOpen(u.id) } }
                }
            }
        }
    }

    if (showFilter) {
        FilterSheet(
            region = region,
            ownership = ownership,
            onDismiss = { showFilter = false },
            onApply = { r, o -> vm.applyFilter(r, o); showFilter = false },
        )
    }
}

@Composable
private fun UniversityCard(u: University, onClick: () -> Unit) {
    val c = Abit.colors
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(abbreviation(u.shortName))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                TitleText(u.name, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = c.muted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    SubtitleText("${u.region} · ${u.ownership.title}", maxLines = 1)
                }
                if (u.programsCount > 0) {
                    SubtitleText(plural(u.programsCount, "освітня програма", "освітні програми", "освітніх програм"), maxLines = 1)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.faint)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    region: String?,
    ownership: Ownership?,
    onDismiss: () -> Unit,
    onApply: (String?, Ownership?) -> Unit,
) {
    val c = Abit.colors
    var r by remember { mutableStateOf(region) }
    var o by remember { mutableStateOf(ownership) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = if (c.isDark) c.bgBottom else c.bgTop) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Фільтр", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Text("Регіон", color = c.muted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AbitChip("Усі", r == null) { r = null }
                Regions.all.forEach { reg -> AbitChip(reg.name, r == reg.code) { r = reg.code } }
            }
            Spacer(Modifier.height(18.dp))
            Text("Форма власності", color = c.muted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Segmented(
                options = listOf<Ownership?>(null) + Ownership.entries,
                selected = o,
                label = { it?.title ?: "Усі" },
                onSelect = { o = it },
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Скинути", { r = null; o = null }, Modifier.weight(1f))
                PrimaryButton("Застосувати", { onApply(r, o) }, Modifier.weight(1.4f))
            }
        }
    }
}

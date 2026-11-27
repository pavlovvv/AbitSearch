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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.HorizontalDivider
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.kpi.abitsearch.data.repository.FavoritesRepository
import ua.kpi.abitsearch.data.repository.ProgramRepository
import ua.kpi.abitsearch.data.repository.RefreshResult
import ua.kpi.abitsearch.domain.model.FavoriteType
import ua.kpi.abitsearch.domain.model.Program
import ua.kpi.abitsearch.domain.model.YearStat
import ua.kpi.abitsearch.ui.appViewModel
import ua.kpi.abitsearch.ui.components.Badge
import ua.kpi.abitsearch.ui.components.CircleIconButton
import ua.kpi.abitsearch.ui.components.CodeTag
import ua.kpi.abitsearch.ui.components.DetailTopBar
import ua.kpi.abitsearch.ui.components.EnrollmentChart
import ua.kpi.abitsearch.ui.components.GlassCard
import ua.kpi.abitsearch.ui.components.OfflineBanner
import ua.kpi.abitsearch.ui.components.SectionLabel
import ua.kpi.abitsearch.ui.components.SkeletonList
import ua.kpi.abitsearch.ui.components.SubtitleText
import ua.kpi.abitsearch.ui.components.TitleText
import ua.kpi.abitsearch.ui.theme.Abit
import ua.kpi.abitsearch.ui.theme.budgetColor
import ua.kpi.abitsearch.ui.theme.contractColor
import ua.kpi.abitsearch.ui.theme.warningColor

class ProgramViewModel(
    private val programId: Long,
    private val programs: ProgramRepository,
    private val favorites: FavoritesRepository,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val program: StateFlow<Program?> = programs.observeProgram(programId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** null - статистика ще не завантажена. */
    val stats: StateFlow<List<YearStat>?> = programs.observeStats(programId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isFavorite: StateFlow<Boolean> = favorites.isFavorite(FavoriteType.PROGRAM, programId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch { _error.value = (programs.refreshProgram(programId) as? RefreshResult.Error)?.message }
    }

    fun toggleFavorite() {
        val p = program.value ?: return
        viewModelScope.launch {
            favorites.toggle(FavoriteType.PROGRAM, p.id, p.name, "${p.universityName} · ${p.level.title}", isFavorite.value)
        }
    }
}

@Composable
fun ProgramScreen(programId: Long, onBack: () -> Unit, onOpenUniversity: (Long) -> Unit, contentPadding: PaddingValues) {
    val vm = appViewModel(key = "program-$programId") { ProgramViewModel(programId, it.programRepository, it.favoritesRepository) }
    val program by vm.program.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val fav by vm.isFavorite.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            DetailTopBar("Освітня програма", onBack) {
                CircleIconButton(
                    if (fav) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    if (fav) "Прибрати з обраного" else "В обране",
                    tint = if (fav) Color(0xFFFBBF24) else Abit.colors.text,
                    onClick = vm::toggleFavorite,
                )
            }
            error?.let { OfflineBanner(it) }
        }
        val p = program
        if (p == null) {
            item { SkeletonList(3) }
            return@LazyColumn
        }
        item { ProgramHeader(p) { onOpenUniversity(p.universityId) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Бюджет, ${p.lastYear}", p.budget.toString(), budgetColor(Abit.colors.isDark), Modifier.weight(1f))
                StatTile("Контракт, ${p.lastYear}", p.contract.toString(), contractColor(Abit.colors.isDark), Modifier.weight(1f))
            }
        }
        val list = stats
        item { SectionLabel("Зарахування за роками") }
        when {
            list == null -> item { SkeletonList(1) }
            list.isEmpty() -> item { SubtitleText("Статистика ще не завантажена") }
            else -> {
                item { GlassCard(Modifier.fillMaxWidth()) { EnrollmentChart(list) } }
                list.lastOrNull()?.let { last ->
                    item { SectionLabel("Форми навчання, ${last.year}") }
                    item { FormsTable(last) }
                }
            }
        }
        item { SectionLabel("Ліцензія") }
        item { LicenseCard(p) }
        item {
            SubtitleText(
                "Джерело: відкриті дані ЄДЕБО (кількість осіб, зарахованих на навчання), імпортовані сервером AbitSearch.",
                Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp), maxLines = 3,
            )
        }
    }
}

@Composable
private fun ProgramHeader(p: Program, onUniversity: () -> Unit) {
    val c = Abit.colors
    Column(Modifier.padding(top = 6.dp)) {
        CodeTag(p.specialtyCode)
        Spacer(Modifier.height(6.dp))
        Text(p.name, color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
        if (!p.name.equals(p.specialtyName, ignoreCase = true)) SubtitleText("Спеціальність: ${p.specialtyName}")
        SubtitleText("${p.level.title} · основа вступу: ${p.base}")
    }
    Spacer(Modifier.height(10.dp))
    GlassCard(Modifier.fillMaxWidth(), radius = 16.dp, padding = 12.dp, onClick = onUniversity) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Заклад освіти", color = c.muted, fontSize = 12.sp)
                TitleText(p.universityName, maxLines = 2)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.faint)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: Color, modifier: Modifier) {
    GlassCard(modifier) {
        Text(label, color = Abit.colors.muted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FormsTable(s: YearStat) {
    val c = Abit.colors
    val rows = listOf(
        Triple("Денна", s.dayBudget, s.dayContract),
        Triple("Заочна", s.partBudget, s.partContract),
        Triple("Вечірня", s.eveningBudget, s.eveningContract),
    )
    GlassCard(Modifier.fillMaxWidth(), padding = 14.dp) {
        Row {
            Text("Форма", color = c.muted, fontSize = 12.sp, modifier = Modifier.weight(1.4f))
            Text("Бюджет", color = c.muted, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            Text("Контракт", color = c.muted, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        rows.forEach { (form, budget, contract) ->
            HorizontalDivider(color = c.outline, modifier = Modifier.padding(vertical = 8.dp))
            Row {
                Text(form, color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1.4f))
                Text(budget.toString(), color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                Text(contract.toString(), color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LicenseCard(p: Program) {
    val c = Abit.colors
    val license = p.license
    GlassCard(Modifier.fillMaxWidth()) {
        if (license == null) {
            SubtitleText("У відкритих даних ліцензій для цієї спеціальності запису не знайдено", maxLines = 3)
            return@GlassCard
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Ліцензований обсяг", color = c.muted, fontSize = 12.sp)
                Text("${license.volume} осіб на рік", color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            if (license.accredited) {
                Badge(license.accreditationUntil?.let { "Акредитовано до $it" } ?: "Акредитовано", budgetColor(c.isDark))
            } else {
                Badge("Без акредитації", warningColor(c.isDark))
            }
        }
        if (license.volume > 0 && p.total > 0) {
            Spacer(Modifier.height(8.dp))
            SubtitleText("Зараховано ${p.total} з ${license.volume} ліцензованих місць спеціальності (${p.lastYear})", maxLines = 2)
        }
    }
}

/** Картка програми у списках закладу та спеціальності. rank - місце в рейтингу закладів. */
@Composable
fun ProgramCard(p: Program, showUniversity: Boolean, rank: Int? = null, latestYear: Int? = null, onClick: () -> Unit) {
    val c = Abit.colors
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            if (rank != null) {
                Text(rank.toString(), color = c.muted, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
            } else {
                CodeTag(p.specialtyCode)
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                TitleText(if (showUniversity) p.universityName else p.name)
                SubtitleText(if (showUniversity) p.name else p.specialtyName)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (latestYear != null && p.lastYear < latestYear) {
                Badge("Останній набір ${p.lastYear}", warningColor(c.isDark))
                Spacer(Modifier.weight(1f))
            } else {
                Text("${p.lastYear}:  ", color = c.muted, fontSize = 13.sp)
                Text("бюджет ${p.budget}", color = budgetColor(c.isDark), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("  ·  ", color = c.faint, fontSize = 13.sp)
                Text("контракт ${p.contract}", color = contractColor(c.isDark), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.faint)
        }
    }
}

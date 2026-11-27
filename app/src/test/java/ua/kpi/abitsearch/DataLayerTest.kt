package ua.kpi.abitsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ua.kpi.abitsearch.data.remote.abit.LicenseDto
import ua.kpi.abitsearch.data.remote.abit.ProgramDto
import ua.kpi.abitsearch.data.remote.abit.UniversityDto
import ua.kpi.abitsearch.data.toDomain
import ua.kpi.abitsearch.data.toEntity
import ua.kpi.abitsearch.domain.model.EducationLevel
import ua.kpi.abitsearch.domain.model.Ownership
import ua.kpi.abitsearch.domain.model.YearStat
import ua.kpi.abitsearch.ui.UiState
import ua.kpi.abitsearch.ui.components.plural
import ua.kpi.abitsearch.ui.listUiState

class DataLayerTest {

    private val program = ProgramDto(
        id = 42, universityId = 174, universityName = "\"КПІ ім. Ігоря Сікорського\"", level = "BACHELOR",
        base = "Повна загальна середня освіта", specialtyCode = "F2", specialtyName = "Інженерія програмного забезпечення",
        specialtyKey = "інженерія програмного забезпечення", programName = "", lastYear = 2025, budget = 30, contract = 13,
    )

    @Test
    fun universityDtoIsMappedWithRegionCodeAndCleanName() {
        val dto = UniversityDto(174, "\"Київський політехнічний інститут\"", null, "Державна", "Київ", "м. Київ", "просп. Берестейський, 37", "", 61)
        val e = dto.toEntity(now = 0)
        assertEquals("Київський політехнічний інститут", e.name)
        assertEquals("Київський політехнічний інститут", e.shortName)
        assertEquals("80", e.regionCode)
        assertNull(e.site)
        assertEquals(61, e.toDomain().programsCount)
    }

    @Test
    fun programWithoutNameUsesSpecialtyAndKeepsLicense() {
        val p = program.toEntity(now = 0, license = LicenseDto(250, true, "01.07.2028")).toDomain()
        assertEquals("Інженерія програмного забезпечення", p.name)
        assertEquals("КПІ ім. Ігоря Сікорського", p.universityName)
        assertEquals(EducationLevel.BACHELOR, p.level)
        assertEquals(43, p.total)
        assertEquals(250, p.license?.volume)
        assertNull(program.toEntity(now = 0).toDomain().license)
    }

    @Test
    fun yearStatSumsAllForms() {
        val s = YearStat(2025, dayBudget = 20, dayContract = 10, partBudget = 3, partContract = 4, eveningBudget = 0, eveningContract = 1)
        assertEquals(23, s.budget)
        assertEquals(15, s.contract)
        assertEquals(38, s.total)
    }

    @Test
    fun ownershipIsParsedFromEdboText() {
        assertEquals(Ownership.PRIVATE, Ownership.fromEdbo("Приватна"))
        assertEquals(Ownership.COMMUNAL, Ownership.fromEdbo("Комунальна"))
        assertEquals(Ownership.STATE, Ownership.fromEdbo("Державна"))
        assertEquals(Ownership.STATE, Ownership.fromEdbo(null))
    }

    @Test
    fun ukrainianPlurals() {
        assertEquals("1 програма", plural(1, "програма", "програми", "програм"))
        assertEquals("3 програми", plural(3, "програма", "програми", "програм"))
        assertEquals("11 програм", plural(11, "програма", "програми", "програм"))
        assertEquals("25 програм", plural(25, "програма", "програми", "програм"))
    }

    @Test
    fun uiStateReflectsCacheAndErrors() {
        assertEquals(UiState.Loading, listUiState(emptyList<Int>(), refreshing = true, error = null))
        assertEquals(UiState.Error("x"), listUiState(emptyList<Int>(), refreshing = false, error = "x"))
        assertEquals(UiState.Success(listOf(1), "x"), listUiState(listOf(1), refreshing = false, error = "x"))
        assertEquals(UiState.Empty, listUiState(emptyList<Int>(), refreshing = false, error = null))
    }
}

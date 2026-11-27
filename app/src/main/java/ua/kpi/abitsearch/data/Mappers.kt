package ua.kpi.abitsearch.data

import ua.kpi.abitsearch.data.local.FavoriteEntity
import ua.kpi.abitsearch.data.local.ProgramEntity
import ua.kpi.abitsearch.data.local.ProgramStatEntity
import ua.kpi.abitsearch.data.local.SpecialtyEntity
import ua.kpi.abitsearch.data.local.UniversityEntity
import ua.kpi.abitsearch.data.remote.abit.LicenseDto
import ua.kpi.abitsearch.data.remote.abit.ProgramDto
import ua.kpi.abitsearch.data.remote.abit.SpecialtyDto
import ua.kpi.abitsearch.data.remote.abit.UniversityDto
import ua.kpi.abitsearch.data.remote.abit.YearStatDto
import ua.kpi.abitsearch.domain.model.EducationLevel
import ua.kpi.abitsearch.domain.model.Favorite
import ua.kpi.abitsearch.domain.model.License
import ua.kpi.abitsearch.domain.model.Ownership
import ua.kpi.abitsearch.domain.model.Program
import ua.kpi.abitsearch.domain.model.Specialty
import ua.kpi.abitsearch.domain.model.University
import ua.kpi.abitsearch.domain.model.YearStat

/* DTO сервера → сутність Room → доменна модель. UI бачить лише доменні моделі. */

fun UniversityDto.toEntity(now: Long): UniversityEntity {
    val region = Regions.byName(region)
    val cleanName = name.orEmpty().cleanQuotes()
    val cleanShort = shortName?.cleanQuotes()?.takeIf { it.isNotBlank() } ?: cleanName
    return UniversityEntity(
        id = id,
        name = cleanName,
        shortName = cleanShort,
        // Починається з назви, тому ORDER BY searchName дає алфавітний порядок без урахування регістру й лапок.
        searchName = "$cleanName $cleanShort".lowercase(),
        regionCode = region?.code ?: "00",
        regionName = this.region ?: region?.name ?: "-",
        ownership = Ownership.fromEdbo(ownership),
        site = site?.takeIf { it.isNotBlank() },
        address = listOfNotNull(city, address).filter { it.isNotBlank() }.joinToString(", ").ifBlank { null },
        programsCount = programsCount,
        updatedAt = now,
    )
}

/** Прибирає пробіли та лапки, що обрамлюють усю назву: "\"Класичний університет\"" → "Класичний університет". */
internal fun String.cleanQuotes(): String {
    var s = trim()
    val quotes = "\"«»“”„'"
    while (s.length > 1 && s.first() in quotes && s.last() in quotes) s = s.substring(1, s.length - 1).trim()
    if (s.count { it == '"' } == 1) s = s.replace("\"", "")
    return s
}

fun UniversityEntity.toDomain() = University(
    id = id, name = name, shortName = shortName, region = regionName,
    ownership = ownership, site = site, address = address, programsCount = programsCount,
)

fun ProgramDto.toEntity(now: Long, license: LicenseDto? = null): ProgramEntity {
    val specialty = specialtyName.orEmpty()
    val title = programName?.takeIf { it.isNotBlank() } ?: specialty
    return ProgramEntity(
        id = id,
        universityId = universityId,
        universityName = universityName.orEmpty().cleanQuotes(),
        level = EducationLevel.from(level),
        base = base.orEmpty(),
        specialtyCode = specialtyCode.orEmpty(),
        specialtyName = specialty,
        specialtyKey = specialtyKey ?: specialty.lowercase(),
        name = title,
        searchName = "$title $specialty".lowercase(),
        lastYear = lastYear,
        budget = budget,
        contract = contract,
        licenseVolume = license?.volume,
        accredited = license?.accredited,
        accreditationUntil = license?.accreditationUntil,
        updatedAt = now,
    )
}

fun ProgramEntity.toDomain() = Program(
    id = id, universityId = universityId, universityName = universityName, level = level, base = base,
    specialtyCode = specialtyCode, specialtyName = specialtyName, specialtyKey = specialtyKey,
    name = name, lastYear = lastYear, budget = budget, contract = contract,
    license = licenseVolume?.let { License(it, accredited == true, accreditationUntil) },
)

fun YearStatDto.toEntity(programId: Long) = ProgramStatEntity(
    programId, year, dayBudget, dayContract, partBudget, partContract, eveningBudget, eveningContract,
)

fun ProgramStatEntity.toDomain() = YearStat(year, dayBudget, dayContract, partBudget, partContract, eveningBudget, eveningContract)

fun SpecialtyDto.toEntity() = SpecialtyEntity(
    key = key, level = EducationLevel.from(level), code = code.orEmpty(), name = name.orEmpty(),
    searchName = name.orEmpty().lowercase(), year = year, programsCount = programsCount,
    universitiesCount = universitiesCount, budget = budget, contract = contract,
)

fun SpecialtyEntity.toDomain() = Specialty(
    key = key, code = code, name = name, level = level, year = year, programsCount = programsCount,
    universitiesCount = universitiesCount, budget = budget, contract = contract,
)

fun FavoriteEntity.toDomain() = Favorite(type = type, refId = refId, title = title, subtitle = subtitle, addedAt = addedAt)

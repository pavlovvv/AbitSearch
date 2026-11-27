package ua.kpi.abitsearch.data

import ua.kpi.abitsearch.domain.model.Region

/** Регіони України з кодами КОАТУУ (перші дві цифри), які приймає API ЄДЕБО у параметрі rg. */
object Regions {
    val all = listOf(
        Region("80", "Київ"),
        Region("46", "Львівська"),
        Region("63", "Харківська"),
        Region("51", "Одеська"),
        Region("12", "Дніпропетровська"),
        Region("05", "Вінницька"),
        Region("07", "Волинська"),
        Region("14", "Донецька"),
        Region("18", "Житомирська"),
        Region("21", "Закарпатська"),
        Region("23", "Запорізька"),
        Region("26", "Івано-Франківська"),
        Region("32", "Київська"),
        Region("35", "Кіровоградська"),
        Region("44", "Луганська"),
        Region("48", "Миколаївська"),
        Region("53", "Полтавська"),
        Region("56", "Рівненська"),
        Region("59", "Сумська"),
        Region("61", "Тернопільська"),
        Region("65", "Херсонська"),
        Region("68", "Хмельницька"),
        Region("71", "Черкаська"),
        Region("73", "Чернівецька"),
        Region("74", "Чернігівська"),
    )

    fun byCode(code: String) = all.firstOrNull { it.code == code }

    /** ЄДЕБО повертає регіон назвою: «Київ», «Львівська» тощо. */
    fun byName(name: String?) = name?.trim()?.let { n -> all.firstOrNull { it.name.equals(n, ignoreCase = true) } }
}

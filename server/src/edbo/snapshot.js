import { createHash } from 'node:crypto';

/** Нормалізація текстів ЄДЕБО: однакові назви з різними лапками, апострофами й пробілами дають один ключ. */
const QUOTES = '"«»“”„\'';

export function clean(s) {
  let v = String(s ?? '').replace(/\s+/g, ' ').trim();
  while (v.length > 1 && QUOTES.includes(v[0]) && QUOTES.includes(v.at(-1))) v = v.slice(1, -1).trim();
  if ((v.match(/"/g) ?? []).length === 1) v = v.replace('"', '');
  return v;
}

export const key = (s) => clean(s).toLowerCase().replace(/[’ʼ`‘]/g, "'");

/** «Львівська область» → «Львівська», як у реєстрі закладів. */
export const region = (s) => clean(s).replace(/ область$/, '').replace(/ обл\.$/, '');

export const parentId = (s) => {
  const n = Number(String(s ?? '').trim());
  return Number.isInteger(n) && n > 0 ? n : null;
};

/**
 * Стабільний ідентифікатор програми з її ключа (48 біт SHA-256): після кожного
 * повторного імпорту програма отримує той самий id і точно вміщується в Number.
 */
export function stableId(k) {
  return parseInt(createHash('sha256').update(k).digest('hex').slice(0, 12), 16);
}

/**
 * Перетворює «сирі» відкриті дані на нормалізовану модель сервера.
 * Чиста функція без мережі й БД, тому її легко перевірити модульними тестами.
 *
 * - Філії приєднуються до головного закладу (Код головного закладу).
 * - Освітня програма впізнається за закладом, ступенем, назвою спеціальності й програми,
 *   а не за кодом спеціальності: у 2025 році коди змінилися (121 → F2), а назви лишилися.
 * - Заклад, що є в даних про вступ, але відсутній у реєстрі, додається як неактивний.
 *
 * @param registry  записи реєстру (mapUniversity)
 * @param batches   [{ year, track: { level, base }, rows: [mapEntrant] }]
 * @param licenses  { BACHELOR: [mapLicense], MASTER: [...] }
 */
export function buildSnapshot(registry, batches, licenses) {
  const byId = new Map(registry.map((u) => [u.id, u]));
  const mainId = (id, rowParent) => {
    const p = parentId(byId.get(id)?.parentId) ?? parentId(rowParent);
    return p !== null && p !== id ? p : id;
  };

  const universities = new Map();
  for (const u of registry) {
    if (mainId(u.id, u.parentId) !== u.id || !clean(u.name)) continue;
    const name = clean(u.name);
    universities.set(u.id, {
      id: u.id,
      name,
      shortName: clean(u.shortName) || name,
      ownership: clean(u.ownership) || null,
      region: region(u.region) || null,
      city: clean(u.city) || null,
      address: clean(u.address) || null,
      site: clean(u.site) || null,
      active: !clean(u.blockedAt),
    });
  }

  const programs = new Map(); // id → { row, stats: Map<year, number[6]> }
  for (const batch of [...batches].sort((a, b) => a.year - b.year)) {
    for (const e of batch.rows) {
      const uni = mainId(e.universityId, e.parentId);
      if (!universities.has(uni)) {
        const name = clean(e.universityName) || `Заклад ${uni}`;
        universities.set(uni, { id: uni, name, shortName: name, ownership: clean(e.ownership) || null, region: region(e.region) || null, city: null, address: null, site: null, active: false });
      }
      const specialty = clean(e.specialtyName) || clean(e.specialtyCode);
      const program = clean(e.program) || specialty;
      const specialization = clean(e.specialization);
      const title = specialization && !program.toLowerCase().includes(specialization.toLowerCase()) ? `${program} (${specialization})` : program;
      const id = stableId([uni, batch.track.level, key(specialty), key(program), key(specialization)].join('|'));

      // Батчі впорядковані за роком, тому назва й код беруться з найсвіжішого запису.
      const entry = programs.get(id) ?? { stats: new Map() };
      entry.row = {
        id, universityId: uni, level: batch.track.level, base: batch.track.base,
        specialtyCode: clean(e.specialtyCode), specialtyName: specialty, specialtyKey: key(specialty),
        programName: title, lastYear: Math.max(entry.row?.lastYear ?? 0, batch.year),
      };
      const c = entry.stats.get(batch.year) ?? [0, 0, 0, 0, 0, 0];
      c[0] += e.dayBudget; c[1] += e.dayContract;
      c[2] += e.partBudget; c[3] += e.partContract;
      c[4] += e.eveningBudget; c[5] += e.eveningContract;
      entry.stats.set(batch.year, c);
      programs.set(id, entry);
    }
  }

  const stats = [];
  for (const { row, stats: byYear } of programs.values()) {
    for (const [year, c] of byYear) {
      stats.push({ programId: row.id, year, dayBudget: c[0], dayContract: c[1], partBudget: c[2], partContract: c[3], eveningBudget: c[4], eveningContract: c[5] });
    }
  }

  // Кілька програм однієї спеціальності - один ліцензований обсяг на спеціальність.
  const licenseGroups = new Map();
  for (const [level, rows] of Object.entries(licenses)) {
    for (const l of rows) {
      const uni = mainId(l.universityId, l.parentId);
      if (!universities.has(uni)) continue;
      const volume = l.volumeYear > 0 ? l.volumeYear : l.volumeDay + l.volumePart + l.volumeEvening;
      const k = `${uni}|${level}|${key(l.specialtyName)}`;
      const g = licenseGroups.get(k) ?? { universityId: uni, level, specialtyCode: clean(l.specialtyCode), specialtyKey: key(l.specialtyName), volume: 0, accredited: false, accreditationUntil: null };
      g.volume += volume;
      g.accredited ||= clean(l.accredited).toLowerCase() === 'так';
      const until = clean(l.accreditationUntil);
      // Дата у форматі дд.мм.рррр: для порівняння переставляємо в рррр.мм.дд.
      const sortable = (d) => (d ? d.split('.').reverse().join('.') : '');
      if (until && sortable(until) > sortable(g.accreditationUntil)) g.accreditationUntil = until;
      licenseGroups.set(k, g);
    }
  }

  return {
    universities: [...universities.values()],
    programs: [...programs.values()].map((p) => p.row),
    stats,
    licenses: [...licenseGroups.values()],
  };
}

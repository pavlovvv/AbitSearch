/** Запити на читання імпортованих даних. Суми й кількості рахує СУБД (GROUP BY, підзапити). */

const PROGRAM_SELECT = `
  SELECT p.*, u.short_name AS university_name,
         COALESCE(s.day_budget + s.part_budget + s.evening_budget, 0) AS budget,
         COALESCE(s.day_contract + s.part_contract + s.evening_contract, 0) AS contract
  FROM programs p
  JOIN universities u ON u.id = p.university_id
  LEFT JOIN program_stats s ON s.program_id = p.id AND s.admission_year = p.last_year`;

const toProgram = (r) => ({
  id: r.id, universityId: r.university_id, universityName: r.university_name,
  level: r.level, base: r.base,
  specialtyCode: r.specialty_code, specialtyName: r.specialty_name, specialtyKey: r.specialty_key,
  programName: r.program_name, lastYear: r.last_year, budget: r.budget, contract: r.contract,
});

export class DataRepository {
  constructor(db) {
    this.db = db;
  }

  /** Останній рік, за який є дані про зарахування. */
  latestYear() {
    return this.db.prepare('SELECT MAX(admission_year) AS y FROM program_stats').get().y ?? null;
  }

  /** Діючі головні заклади та заклади з програмами останнього набору. */
  universities() {
    const year = this.latestYear() ?? 0;
    return this.db.prepare(`
      SELECT u.*, (SELECT COUNT(*) FROM programs p WHERE p.university_id = u.id AND p.last_year = ?) AS programs_count
      FROM universities u
      WHERE u.active = 1 OR EXISTS (SELECT 1 FROM programs p WHERE p.university_id = u.id AND p.last_year = ?)
      ORDER BY u.name`).all(year, year)
      .map((r) => ({
        id: r.id, name: r.name, shortName: r.short_name, ownership: r.ownership, region: r.region,
        city: r.city, address: r.address, site: r.site, programsCount: r.programs_count,
      }));
  }

  universityExists(id) {
    return this.db.prepare('SELECT 1 FROM universities WHERE id = ?').get(id) !== undefined;
  }

  programsOfUniversity(id) {
    return this.db.prepare(`${PROGRAM_SELECT} WHERE p.university_id = ?
      ORDER BY p.level, p.last_year DESC, budget + contract DESC`).all(id).map(toProgram);
  }

  programsOfSpecialty(specialtyKey, level) {
    const year = this.latestYear();
    if (year === null) return [];
    return this.db.prepare(`${PROGRAM_SELECT} WHERE p.specialty_key = ? AND p.level = ? AND p.last_year = ?
      ORDER BY budget + contract DESC`).all(specialtyKey, level, year).map(toProgram);
  }

  program(id) {
    const row = this.db.prepare(`${PROGRAM_SELECT} WHERE p.id = ?`).get(id);
    if (!row) return null;
    const program = toProgram(row);
    const stats = this.db.prepare('SELECT * FROM program_stats WHERE program_id = ? ORDER BY admission_year').all(id)
      .map((s) => ({
        year: s.admission_year, dayBudget: s.day_budget, dayContract: s.day_contract,
        partBudget: s.part_budget, partContract: s.part_contract,
        eveningBudget: s.evening_budget, eveningContract: s.evening_contract,
      }));
    const l = this.db.prepare('SELECT volume, accredited, accreditation_until FROM licenses WHERE university_id = ? AND level = ? AND specialty_key = ?')
      .get(program.universityId, program.level, program.specialtyKey);
    const license = l ? { volume: l.volume, accredited: l.accredited === 1, accreditationUntil: l.accreditation_until } : null;
    return { program, stats, license };
  }

  /** Спеціальності останнього набору з кількістю програм, закладів і зарахованих. */
  specialties(level) {
    const year = this.latestYear();
    if (year === null) return [];
    return this.db.prepare(`
      SELECT p.specialty_key, MAX(p.specialty_code) AS code, MAX(p.specialty_name) AS name,
             COUNT(*) AS programs_count, COUNT(DISTINCT p.university_id) AS universities_count,
             SUM(s.day_budget + s.part_budget + s.evening_budget) AS budget,
             SUM(s.day_contract + s.part_contract + s.evening_contract) AS contract
      FROM programs p JOIN program_stats s ON s.program_id = p.id AND s.admission_year = p.last_year
      WHERE p.level = ? AND p.last_year = ?
      GROUP BY p.specialty_key
      ORDER BY name`).all(level, year)
      .map((r) => ({
        key: r.specialty_key, code: r.code, name: r.name, level, year,
        programsCount: r.programs_count, universitiesCount: r.universities_count, budget: r.budget, contract: r.contract,
      }));
  }

  lastImport() {
    const r = this.db.prepare('SELECT * FROM import_log ORDER BY id DESC LIMIT 1').get();
    return r ? {
      startedAt: r.started_at, finishedAt: r.finished_at, status: r.status, universities: r.universities,
      programs: r.programs, stats: r.stats, licenses: r.licenses, message: r.message,
    } : null;
  }

  meta(importRunning) {
    const count = (t) => this.db.prepare(`SELECT COUNT(*) AS n FROM ${t}`).get().n;
    const { t } = this.db.prepare("SELECT MAX(finished_at) AS t FROM import_log WHERE status = 'SUCCESS'").get();
    return {
      source: 'Відкриті дані ЄДЕБО, registry.edbo.gov.ua/opendata',
      status: importRunning ? 'IMPORTING' : t == null ? 'EMPTY' : 'READY',
      lastImportAt: t ?? null, latestYear: this.latestYear(),
      universities: count('universities'), programs: count('programs'),
      stats: count('program_stats'), licenses: count('licenses'),
    };
  }
}

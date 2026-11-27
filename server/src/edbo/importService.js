import { transaction } from '../db/database.js';
import { buildSnapshot } from './snapshot.js';
import { TRACKS, LEVELS } from './client.js';

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

/**
 * Імпорт відкритих даних ЄДЕБО в БД сервера.
 * 1) реєстр закладів; 2) зарахування за кожен рік і набір; 3) ліцензовані спеціальності.
 * Дані - повний знімок, тому запис іде стратегією «замінити все в одній транзакції»:
 * клієнти ніколи не бачать напівімпортованого стану, а при помилці лишаються старі дані.
 */
export class ImportService {
  constructor(db, client, config, { pauseMs = 300, log = console, now = Date.now } = {}) {
    Object.assign(this, { db, client, config, pauseMs, log, now });
    this.running = false;
    // Якщо сервер зупинили посеред імпорту, запис лишився в статусі RUNNING.
    db.prepare("UPDATE import_log SET status = 'FAILED', message = 'Перервано перезапуском' WHERE status = 'RUNNING'").run();
  }

  /** Даних ще немає або останній успішний імпорт застарів. */
  isDue() {
    const { t } = this.db.prepare("SELECT MAX(finished_at) AS t FROM import_log WHERE status = 'SUCCESS'").get();
    return this.now() - (t ?? 0) > this.config.importIntervalHours * 3_600_000;
  }

  /** Повертає false, якщо інший імпорт уже виконується. */
  async run() {
    if (this.running) return false;
    this.running = true;
    const { lastInsertRowid: logId } = this.db.prepare("INSERT INTO import_log(started_at, status) VALUES (?, 'RUNNING')").run(this.now());
    try {
      this.log.info('Імпорт ЄДЕБО: реєстр закладів');
      const registry = await this.client.universities();
      if (registry.length === 0) throw new Error('Реєстр закладів порожній');

      const batches = [];
      for (let year = this.config.fromYear; year <= this.config.toYear; year++) {
        for (const track of TRACKS) {
          await sleep(this.pauseMs); // не навантажуємо державний сервіс паралельними запитами
          const rows = await this.client.entrants(year, track);
          this.log.info(`Імпорт ЄДЕБО: ${year} ${LEVELS[track.level].title} - ${rows.length} записів`);
          batches.push({ year, track, rows });
        }
      }
      if (!batches.some((b) => b.rows.length > 0)) throw new Error('Дані про зарахування порожні');

      const licenses = {};
      for (const level of Object.keys(LEVELS)) {
        await sleep(this.pauseMs);
        licenses[level] = await this.client.licenses(level);
      }

      const s = buildSnapshot(registry, batches, licenses);
      transaction(this.db, () => this.#write(s));
      this.db.prepare("UPDATE import_log SET finished_at = ?, status = 'SUCCESS', universities = ?, programs = ?, stats = ?, licenses = ? WHERE id = ?")
        .run(this.now(), s.universities.length, s.programs.length, s.stats.length, s.licenses.length, logId);
      this.log.info(`Імпорт завершено: ${s.universities.length} закладів, ${s.programs.length} програм, ${s.stats.length} записів статистики`);
    } catch (e) {
      this.log.error('Імпорт ЄДЕБО не вдався:', e);
      this.db.prepare("UPDATE import_log SET finished_at = ?, status = 'FAILED', message = ? WHERE id = ?")
        .run(this.now(), String(e.message ?? e).slice(0, 1900), logId);
    } finally {
      this.running = false;
    }
    return true;
  }

  #write(s) {
    for (const t of ['program_stats', 'programs', 'licenses', 'universities']) this.db.exec(`DELETE FROM ${t}`);
    const u = this.db.prepare('INSERT INTO universities(id, name, short_name, ownership, region, city, address, site, active) VALUES (?,?,?,?,?,?,?,?,?)');
    for (const x of s.universities) u.run(x.id, x.name, x.shortName, x.ownership, x.region, x.city, x.address, x.site, x.active ? 1 : 0);
    const p = this.db.prepare('INSERT INTO programs(id, university_id, level, base, specialty_code, specialty_name, specialty_key, program_name, last_year) VALUES (?,?,?,?,?,?,?,?,?)');
    for (const x of s.programs) p.run(x.id, x.universityId, x.level, x.base, x.specialtyCode, x.specialtyName, x.specialtyKey, x.programName, x.lastYear);
    const st = this.db.prepare('INSERT INTO program_stats(program_id, admission_year, day_budget, day_contract, part_budget, part_contract, evening_budget, evening_contract) VALUES (?,?,?,?,?,?,?,?)');
    for (const x of s.stats) st.run(x.programId, x.year, x.dayBudget, x.dayContract, x.partBudget, x.partContract, x.eveningBudget, x.eveningContract);
    const l = this.db.prepare('INSERT INTO licenses(university_id, level, specialty_code, specialty_key, volume, accredited, accreditation_until) VALUES (?,?,?,?,?,?,?)');
    for (const x of s.licenses) l.run(x.universityId, x.level, x.specialtyCode, x.specialtyKey, x.volume, x.accredited ? 1 : 0, x.accreditationUntil);
  }
}

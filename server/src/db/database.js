import { DatabaseSync } from 'node:sqlite';
import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';

/**
 * Реляційна БД сервера - SQLite через вбудований модуль node:sqlite (Node.js 22.5+),
 * тож окремо встановлювати СУБД не потрібно. Схема створюється при старті.
 */
const SCHEMA = `
PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;

CREATE TABLE IF NOT EXISTS users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  email TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('USER', 'ADMIN')),
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS universities (
  id INTEGER PRIMARY KEY,               -- код ЄДЕБО
  name TEXT NOT NULL,
  short_name TEXT NOT NULL,
  ownership TEXT,
  region TEXT,
  city TEXT,
  address TEXT,
  site TEXT,
  active INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS programs (
  id INTEGER PRIMARY KEY,               -- стабільний хеш ключа програми
  university_id INTEGER NOT NULL REFERENCES universities(id) ON DELETE CASCADE,
  level TEXT NOT NULL,
  base TEXT NOT NULL,
  specialty_code TEXT NOT NULL,
  specialty_name TEXT NOT NULL,
  specialty_key TEXT NOT NULL,
  program_name TEXT NOT NULL,
  last_year INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_programs_university ON programs(university_id);
CREATE INDEX IF NOT EXISTS idx_programs_specialty ON programs(specialty_key, level, last_year);

CREATE TABLE IF NOT EXISTS program_stats (
  program_id INTEGER NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
  admission_year INTEGER NOT NULL,
  day_budget INTEGER NOT NULL,
  day_contract INTEGER NOT NULL,
  part_budget INTEGER NOT NULL,
  part_contract INTEGER NOT NULL,
  evening_budget INTEGER NOT NULL,
  evening_contract INTEGER NOT NULL,
  PRIMARY KEY (program_id, admission_year)
);

CREATE TABLE IF NOT EXISTS licenses (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  university_id INTEGER NOT NULL REFERENCES universities(id) ON DELETE CASCADE,
  level TEXT NOT NULL,
  specialty_code TEXT NOT NULL,
  specialty_key TEXT NOT NULL,
  volume INTEGER NOT NULL,
  accredited INTEGER NOT NULL,
  accreditation_until TEXT
);
CREATE INDEX IF NOT EXISTS idx_licenses_lookup ON licenses(university_id, level, specialty_key);

CREATE TABLE IF NOT EXISTS import_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  started_at INTEGER NOT NULL,
  finished_at INTEGER,
  status TEXT NOT NULL,
  universities INTEGER NOT NULL DEFAULT 0,
  programs INTEGER NOT NULL DEFAULT 0,
  stats INTEGER NOT NULL DEFAULT 0,
  licenses INTEGER NOT NULL DEFAULT 0,
  message TEXT
);
`;

export function openDatabase(file) {
  if (file !== ':memory:') mkdirSync(dirname(file), { recursive: true });
  const db = new DatabaseSync(file);
  db.exec(SCHEMA);
  return db;
}

/** Виконує fn в одній транзакції: або всі зміни, або жодної. */
export function transaction(db, fn) {
  db.exec('BEGIN');
  try {
    const result = fn();
    db.exec('COMMIT');
    return result;
  } catch (e) {
    db.exec('ROLLBACK');
    throw e;
  }
}
